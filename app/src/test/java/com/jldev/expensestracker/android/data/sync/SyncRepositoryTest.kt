package com.jldev.expensestracker.android.data.sync

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jldev.expensestracker.android.data.T0
import com.jldev.expensestracker.android.data.cycle
import com.jldev.expensestracker.android.data.expense
import com.jldev.expensestracker.android.data.local.AppDatabase
import com.jldev.expensestracker.android.data.remote.ApiJson
import kotlinx.coroutines.flow.first
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@RunWith(RobolectricTestRunner::class)
class SyncRepositoryTest {

    private val server = MockWebServer()
    private lateinit var database: AppDatabase
    private lateinit var store: SyncStore
    private lateinit var repository: SyncRepository

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
        repository = SyncRepository(api, database, store)
    }

    @After
    fun tearDown() {
        server.close()
        database.close()
    }

    private fun enqueue(body: String) = server.enqueue(
        MockResponse.Builder().addHeader("Content-Type", "application/json").body(body).build(),
    )

    private fun record(uuid: String, extra: String) =
        """{"uuid": "$uuid", "sync_version": 1, "created_at": "2026-09-01T12:00:00",
            "updated_at": "2026-09-01T12:00:00", "deleted_at": null, $extra}"""

    private val september = record(
        "sep", """"description": "September", "start_date": "2026-09-01", "end_date": "2026-09-30", "is_active": true""",
    )

    private fun expenseJson(uuid: String, deleted: Boolean = false) = record(
        uuid,
        """"description": "$uuid", "val_expense": 10.0, "date_expense": "2026-09-02T15:30:00", "source": "App",
           "categories": "", "is_recurrent_expense": false, "cycle_uuid": "sep", "budget_uuid": null,
           "recurrent_expense_uuid": null""",
    ).let { if (deleted) it.replace("\"deleted_at\": null", "\"deleted_at\": \"2026-09-03T00:00:00\"") else it }

    @Test
    fun `sync pulls every page, removes deletions and saves the cursor`() = runTest {
        database.syncDao().upsertExpenses(listOf(expense("gone", "sep", 1.0)))
        enqueue("""{"cursor": 10, "has_more": true, "changes": {"cycles": [$september], "expenses": [${expenseJson("taxi")}]}}""")
        enqueue("""{"cursor": 25, "has_more": false, "changes": {"expenses": [${expenseJson("gone", deleted = true)}]}}""")

        repository.sync()

        assertEquals("/prod/sync?since=0&limit=500", server.takeRequest().target)
        assertEquals("/prod/sync?since=10&limit=500", server.takeRequest().target)
        assertEquals(25L, store.cursor())
        val expenses = database.expenseDao().expenses("sep", null, false).first()
        assertEquals(listOf("taxi"), expenses.map { it.expense.uuid })
        // Naive backend datetimes are UTC.
        assertEquals(java.time.Instant.parse("2026-09-02T15:30:00Z"), expenses.single().expense.dateExpense)
        assertTrue(database.cycleDao().cycles().first().single().isActive)
    }

    @Test
    fun `the next sync starts from the saved cursor`() = runTest {
        store.setCursor(42)
        enqueue("""{"cursor": 42, "has_more": false, "changes": {}}""")
        repository.sync()
        assertEquals("/prod/sync?since=42&limit=500", server.takeRequest().target)
    }

    @Test
    fun `push sends whole records with explicit nulls and stores the server's copies`() = runTest {
        val newExpense = expense("taxi", "sep", 10.0).copy(updatedAt = T0)
        enqueue("""{"results": [{"uuid": "taxi", "status": "applied", "record": ${expenseJson("taxi")}}]}""")
        enqueue("""{"cursor": 5, "has_more": false, "changes": {}}""")

        repository.push(listOf(SyncTables.expenses.change(newExpense)))

        val body = ApiJson.parseToJsonElement(server.takeRequest().body!!.utf8()).jsonObject
        val change = body["changes"]!!.jsonArray.single().jsonObject
        assertEquals("expenses", change["entity"]!!.jsonPrimitive.content)
        assertEquals("2026-09-01T12:00:00Z", change["updated_at"]!!.jsonPrimitive.content)
        val data = change["data"]!!.jsonObject
        // A null reference is sent, so the server clears it; server-managed fields aren't.
        assertEquals(JsonNull, data["budget_uuid"])
        assertFalse("uuid" in data || "sync_version" in data || "updated_at" in data)
        assertEquals(listOf("taxi"), database.expenseDao().expenses("sep", null, false).first().map { it.expense.uuid })
        // Then it pulled.
        assertEquals("/prod/sync?since=0&limit=500", server.takeRequest().target)
    }

    @Test
    fun `cycles are pushed without the server-managed active flag`() {
        val data = SyncTables.cycles.change(cycle("oct", "2026-10-01", active = true)).data
        assertFalse("is_active" in data)
        assertEquals("2026-10-01", data["start_date"]!!.jsonPrimitive.content)
    }

    @Test
    fun `a merged record replaces the local one`() = runTest {
        database.syncDao().upsertCycles(listOf(cycle("offline-sep", "2026-09-01")))
        enqueue("""{"results": [{"uuid": "offline-sep", "status": "merged", "record": $september}]}""")
        enqueue("""{"cursor": 5, "has_more": false, "changes": {}}""")

        repository.push(listOf(SyncTables.cycles.change(cycle("offline-sep", "2026-09-01"))))

        assertEquals(listOf("sep"), database.cycleDao().cycles().first().map { it.uuid })
    }

    @Test
    fun `a rejected change is reported after storing the others`() = runTest {
        enqueue(
            """{"results": [
                {"uuid": "taxi", "status": "applied", "record": ${expenseJson("taxi")}},
                {"uuid": "bad", "status": "rejected", "reason": "Unknown cycle_uuid x"}]}""",
        )
        enqueue("""{"cursor": 5, "has_more": false, "changes": {}}""")

        try {
            repository.push(
                listOf(
                    SyncTables.expenses.change(expense("taxi", "sep", 10.0)),
                    SyncTables.expenses.change(expense("bad", "x", 1.0)),
                ),
            )
            fail("Expected SyncRejectedException")
        } catch (e: SyncRejectedException) {
            assertEquals("Unknown cycle_uuid x", e.reason)
        }
        assertEquals(listOf("taxi"), database.expenseDao().expenses("sep", null, false).first().map { it.expense.uuid })
    }

    @Test
    fun `clear forgets the data and the cursor`() = runTest {
        database.syncDao().upsertCycles(listOf(cycle("sep", "2026-09-01")))
        store.setCursor(99)
        repository.clear()
        assertTrue(database.cycleDao().cycles().first().isEmpty())
        assertEquals(0L, store.cursor())
        assertNull(null)
    }
}
