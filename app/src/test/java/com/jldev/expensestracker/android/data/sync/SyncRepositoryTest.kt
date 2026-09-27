package com.jldev.expensestracker.android.data.sync

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jldev.expensestracker.android.data.budget
import com.jldev.expensestracker.android.data.cycle
import com.jldev.expensestracker.android.data.expense
import com.jldev.expensestracker.android.data.local.AppDatabase
import com.jldev.expensestracker.android.data.remote.ApiJson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SyncRepositoryTest {

    private class FakeScheduler : SyncScheduler {
        var syncSoonCalls = 0
        override fun syncSoon() {
            syncSoonCalls++
        }
        override fun startPeriodic() = Unit
        override fun stop() = Unit
    }

    private val server = MockWebServer()
    private val scheduler = FakeScheduler()
    private lateinit var database: AppDatabase
    private lateinit var store: SyncStore
    private lateinit var repository: SyncRepository

    // Cycles relative to today, so the 6-month window keeps "current" and drops "old".
    private val currentStart = LocalDate.now().withDayOfMonth(1)
    private val oldStart = currentStart.minusMonths(12)

    @Before
    fun setUp() = runTest {
        server.start()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/prod/"))
            .addConverterFactory(ApiJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SyncApi::class.java)
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        store = SyncStore(ApplicationProvider.getApplicationContext())
        store.setCursor(0)
        repository = SyncRepository(api, database, store, scheduler)
    }

    @After
    fun tearDown() {
        server.close()
        database.close()
    }

    private fun enqueue(body: String, code: Int = 200) = server.enqueue(
        MockResponse.Builder().code(code).addHeader("Content-Type", "application/json").body(body).build(),
    )

    private fun enqueueEmptyPull(cursor: Int = 5) = enqueue("""{"cursor": $cursor, "has_more": false, "changes": {}}""")

    private fun record(uuid: String, extra: String) =
        """{"uuid": "$uuid", "sync_version": 1, "created_at": "2026-09-01T12:00:00",
            "updated_at": "2026-09-01T12:00:00", "deleted_at": null, $extra}"""

    private fun cycleJson(uuid: String, start: LocalDate) = record(
        uuid,
        """"description": "$uuid", "start_date": "$start", "end_date": "${start.plusMonths(1).minusDays(1)}",
           "is_active": true""",
    )

    private fun expenseJson(uuid: String, amount: Double = 10.0, cycle: String = "current", deleted: Boolean = false) =
        record(
            uuid,
            """"description": "$uuid", "val_expense": $amount, "date_expense": "2026-09-02T15:30:00", "source": "App",
               "categories": "", "is_recurrent_expense": false, "cycle_uuid": "$cycle", "budget_uuid": null,
               "recurrent_expense_uuid": null""",
        ).let { if (deleted) it.replace("\"deleted_at\": null", "\"deleted_at\": \"2026-09-03T00:00:00\"") else it }

    private suspend fun localExpenses(cycle: String = "current") =
        database.expenseDao().expenses(cycle, null, false).first().map { it.expense }

    private suspend fun pendingCount() = repository.pendingCount.first()

    @Test
    fun `sync pulls every page within the window, removes deletions and saves the cursor`() = runTest {
        database.syncDao().upsertExpenses(listOf(expense("gone", "current", 1.0)))
        enqueue(
            """{"cursor": 10, "has_more": true, "changes": {"cycles": [${cycleJson("current", currentStart)}],
                "expenses": [${expenseJson("taxi")}]}}""",
        )
        enqueue("""{"cursor": 25, "has_more": false, "changes": {"expenses": [${expenseJson("gone", deleted = true)}]}}""")

        repository.sync()

        val window = SyncRepository.windowStart()
        assertEquals("/prod/sync?since=0&window_start=$window&limit=500", server.takeRequest().target)
        assertEquals("/prod/sync?since=10&window_start=$window&limit=500", server.takeRequest().target)
        assertEquals(25L, store.cursor())
        assertEquals(listOf("taxi"), localExpenses().map { it.uuid })
        // Naive backend datetimes are UTC.
        assertEquals(java.time.Instant.parse("2026-09-02T15:30:00Z"), localExpenses().single().dateExpense)
    }

    @Test
    fun `the window keeps six monthly cycles`() {
        assertEquals(LocalDate.of(2026, 4, 1), SyncRepository.windowStart(LocalDate.of(2026, 9, 26)))
    }

    @Test
    fun `saving applies the change locally and queues it without sending anything`() = runTest {
        repository.save(listOf(SyncTables.expenses.save(expense("taxi", "current", 10.0))))

        assertEquals(listOf("taxi"), localExpenses().map { it.uuid })
        assertEquals(1, pendingCount())
        assertEquals(1, scheduler.syncSoonCalls)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `sync pushes the queue in order, then pulls`() = runTest {
        repository.save(listOf(SyncTables.expenses.save(expense("taxi", "current", 10.0))))
        repository.save(listOf(SyncTables.expenses.save(expense("taxi", "current", 12.0))))
        enqueue(
            """{"results": [
                {"uuid": "taxi", "status": "applied", "record": ${expenseJson("taxi", 10.0)}},
                {"uuid": "taxi", "status": "applied", "record": ${expenseJson("taxi", 12.0)}}]}""",
        )
        enqueueEmptyPull()

        repository.sync()

        val changes = ApiJson.parseToJsonElement(server.takeRequest().body!!.utf8()).jsonObject["changes"]!!.jsonArray
        assertEquals(listOf("10.0", "12.0"), changes.map { it.jsonObject["data"]!!.jsonObject["val_expense"].toString() })
        // A null reference is sent, so the server clears it; server-managed fields aren't.
        val data = changes.first().jsonObject["data"]!!.jsonObject
        assertEquals(JsonNull, data["budget_uuid"])
        assertFalse("uuid" in data || "sync_version" in data || "updated_at" in data)
        assertTrue(server.takeRequest().target.startsWith("/prod/sync?since=0"))
        assertEquals(0, pendingCount())
        assertEquals(12.0, localExpenses().single().valExpense, 0.0)
    }

    @Test
    fun `offline, the queue and the local change are kept`() = runTest {
        repository.save(listOf(SyncTables.expenses.save(expense("taxi", "current", 10.0))))
        enqueue("""{"detail": "unavailable"}""", code = 503)

        try {
            repository.sync()
            fail("Expected HttpException")
        } catch (e: HttpException) {
            assertEquals(503, e.code())
        }
        assertEquals(1, pendingCount())
        assertEquals(listOf("taxi"), localExpenses().map { it.uuid })
    }

    @Test
    fun `a pull doesn't overwrite a record with changes still queued`() = runTest {
        repository.save(listOf(SyncTables.expenses.save(expense("taxi", "current", 99.0))))
        // The push fails, so the pull below can't happen in the same sync; pull directly by loading a cycle.
        enqueue("""{"changes": {"expenses": [${expenseJson("taxi", 10.0)}]}}""")

        repository.loadCycle("current")

        assertEquals(99.0, localExpenses().single().valExpense, 0.0)
    }

    @Test
    fun `a merged record replaces the local one, and references follow it`() = runTest {
        repository.save(
            listOf(
                SyncTables.cycles.save(cycle("offline", currentStart.toString())),
                SyncTables.expenses.save(expense("taxi", "offline", 10.0)),
            ),
        )
        // The server already had this month's cycle, under the uuid "current".
        enqueue(
            """{"results": [
                {"uuid": "offline", "status": "merged", "record": ${cycleJson("current", currentStart)}},
                {"uuid": "taxi", "status": "applied", "record": ${expenseJson("taxi")}}]}""",
        )
        enqueueEmptyPull()

        repository.sync()

        assertEquals(listOf("current"), database.cycleDao().cycles().first().map { it.uuid })
        assertEquals(listOf("taxi"), localExpenses("current").map { it.uuid })
    }

    @Test
    fun `queued changes still referencing a merged uuid are rewritten`() = runTest {
        repository.save(listOf(SyncTables.cycles.save(cycle("offline", currentStart.toString()))))
        repository.save(listOf(SyncTables.expenses.save(expense("taxi", "offline", 10.0))))
        // The server answers only the cycle (as if the expense came in a later batch), so the expense stays
        // queued and is pushed again.
        enqueue("""{"results": [{"uuid": "offline", "status": "merged", "record": ${cycleJson("current", currentStart)}}]}""")
        enqueue("""{"results": [{"uuid": "taxi", "status": "applied", "record": ${expenseJson("taxi")}}]}""")
        enqueueEmptyPull()

        repository.sync()

        server.takeRequest()
        val second = ApiJson.parseToJsonElement(server.takeRequest().body!!.utf8()).jsonObject["changes"]!!.jsonArray
        assertEquals("current", second.single().jsonObject["data"]!!.jsonObject["cycle_uuid"]!!.jsonPrimitive.content)
        assertEquals("current", localExpenses("current").single().cycleUuid)
    }

    @Test
    fun `a rejected change is undone and reported`() = runTest {
        database.syncDao().upsertBudgets(listOf(budget("food", "current", 100.0)))
        repository.save(
            listOf(
                SyncTables.budgets.save(budget("food", "current", 999.0)),
                SyncTables.expenses.save(expense("bad", "current", 1.0)),
            ),
        )
        enqueue(
            """{"results": [
                {"uuid": "food", "status": "rejected", "reason": "Invalid data",
                 "record": ${record("food", """"description": "food", "val_budget": 100.0, "cycle_uuid": "current",
                    "recurrent_budget_uuid": null""")}},
                {"uuid": "bad", "status": "rejected", "reason": "Unknown cycle_uuid", "record": null}]}""",
        )
        enqueueEmptyPull()
        val rejections = mutableListOf<SyncRejection>()
        val collecting = launch(UnconfinedTestDispatcher(testScheduler)) { repository.rejections.toList(rejections) }

        repository.sync()
        collecting.cancel()

        // Restored to the server's copy, or removed when the server doesn't have it.
        assertEquals(100.0, database.budgetDao().budgetTotals("current").first().single().valBudget, 0.0)
        assertTrue(localExpenses().isEmpty())
        assertEquals(listOf("Invalid data", "Unknown cycle_uuid"), rejections.map { it.reason })
    }

    @Test
    fun `deleting a budget locally unlinks its expenses, like the server does`() = runTest {
        database.syncDao().upsertBudgets(listOf(budget("food", "current")))
        database.syncDao().upsertExpenses(listOf(expense("lunch", "current", 10.0, budget = "food")))

        repository.save(listOf(SyncTables.budgets.delete(budget("food", "current"))))

        assertEquals(null, localExpenses().single().budgetUuid)
    }

    @Test
    fun `the first sync prunes cycles older than the window, except queued changes`() = runTest {
        database.syncDao().upsertCycles(listOf(cycle("old", oldStart.toString()), cycle("current", currentStart.toString())))
        database.syncDao().upsertExpenses(listOf(expense("old-1", "old", 1.0), expense("now", "current", 1.0)))
        repository.save(listOf(SyncTables.expenses.save(expense("old-edited", "old", 2.0))))
        enqueue("""{"results": [{"uuid": "old-edited", "status": "applied", "record": null}]}""")
        enqueueEmptyPull()

        repository.sync()

        assertEquals(listOf("now"), localExpenses("current").map { it.uuid })
        // The old cycle's records are gone; the one with a queued change was pushed, then pruned too.
        assertTrue(localExpenses("old").isEmpty())
    }

    @Test
    fun `loading an older cycle stores its records`() = runTest {
        enqueue("""{"changes": {"expenses": [${expenseJson("old-taxi", cycle = "old")}]}}""")

        repository.loadCycle("old")

        assertEquals("/prod/sync/cycles/old", server.takeRequest().target)
        assertEquals(listOf("old-taxi"), localExpenses("old").map { it.uuid })
    }

    @Test
    fun `cycles are pushed without the server-managed active flag`() {
        val data = SyncTables.cycles.save(cycle("oct", "2026-10-01", active = true)).change.data
        assertFalse("is_active" in data)
        assertEquals("2026-10-01", data["start_date"]!!.jsonPrimitive.content)
    }

    @Test
    fun `clear forgets the data, the queue and the cursor`() = runTest {
        repository.save(listOf(SyncTables.cycles.save(cycle("current", currentStart.toString()))))
        store.setCursor(99)
        repository.clear()
        assertTrue(database.cycleDao().cycles().first().isEmpty())
        assertEquals(0, pendingCount())
        assertEquals(0L, store.cursor())
    }
}
