package com.jldev.expensestracker.android.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jldev.expensestracker.android.data.budget
import com.jldev.expensestracker.android.data.cycle
import com.jldev.expensestracker.android.data.expense
import com.jldev.expensestracker.android.data.income
import com.jldev.expensestracker.android.data.model.CycleStatus
import com.jldev.expensestracker.android.data.saving
import com.jldev.expensestracker.android.data.savingType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

/** The summary queries must give what the backend's SQL gives for the same data. */
@RunWith(RobolectricTestRunner::class)
class DaoTest {

    private lateinit var database: AppDatabase
    private val sync get() = database.syncDao()

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `cycle status sums the cycle's movements like cycle-status`() = runTest {
        sync.upsertCycles(listOf(cycle("sep", "2026-09-01"), cycle("aug", "2026-08-01")))
        sync.upsertExpenses(
            listOf(
                expense("rent", "sep", 500.0, recurrent = true),
                expense("lunch", "sep", 40.0),
                expense("taxi", "sep", 10.0),
                expense("old", "aug", 999.0),
            ),
        )
        sync.upsertIncomes(listOf(income("salary", "sep", 2_000.0)))
        sync.upsertSavingTypes(listOf(savingType("car", "Car")))
        // Like the backend, withdrawals count towards the cycle's savings too.
        sync.upsertSavings(listOf(saving("s1", "sep", "car", 300.0), saving("s2", "sep", "car", 50.0, outcome = true)))

        assertEquals(
            CycleStatus(totalRecurrentExpenses = 500.0, totalExpenses = 50.0, totalIncomes = 2_000.0, totalSavings = 350.0),
            database.cycleDao().status("sep").first(),
        )
    }

    @Test
    fun `budget totals count the cycle's expenses, most used first`() = runTest {
        sync.upsertBudgets(listOf(budget("food", "sep", 800.0), budget("fun", "sep", 100.0)))
        sync.upsertExpenses(
            listOf(
                expense("a", "sep", 10.0, budget = "fun"),
                expense("b", "sep", 20.0, budget = "food"),
                expense("c", "sep", 30.0, budget = "food"),
                expense("d", "sep", 5.0),
                expense("other-cycle", "aug", 1_000.0, budget = "food"),
            ),
        )
        val budgets = database.budgetDao()

        assertEquals(
            listOf("food" to 50.0, "fun" to 10.0),
            budgets.budgetTotals("sep").first().map { it.uuid to it.totalSpent },
        )
        assertEquals(5.0, budgets.unbudgetedTotal("sep").first(), 0.0)
    }

    @Test
    fun `expenses filter by budget or by having none, newest first`() = runTest {
        sync.upsertBudgets(listOf(budget("food", "sep")))
        sync.upsertExpenses(
            listOf(
                expense("older", "sep", 1.0, budget = "food", date = Instant.parse("2026-09-02T00:00:00Z")),
                expense("newer", "sep", 1.0, budget = "food", date = Instant.parse("2026-09-03T00:00:00Z")),
                expense("none", "sep", 1.0, date = Instant.parse("2026-09-01T00:00:00Z")),
            ),
        )
        val dao = database.expenseDao()

        val all = dao.expenses("sep", budgetUuid = null, unbudgetedOnly = false).first()
        assertEquals(listOf("newer", "older", "none"), all.map { it.expense.uuid })
        assertEquals("food", all.first().budgetDescription)
        assertEquals(
            listOf("newer", "older"),
            dao.expenses("sep", budgetUuid = "food", unbudgetedOnly = false).first().map { it.expense.uuid },
        )
        assertEquals(
            listOf("none"),
            dao.expenses("sep", budgetUuid = null, unbudgetedOnly = true).first().map { it.expense.uuid },
        )
    }

    @Test
    fun `grouped savings total every movement and the active cycle's`() = runTest {
        sync.upsertCycles(listOf(cycle("sep", "2026-09-01", active = true), cycle("aug", "2026-08-01")))
        sync.upsertSavingTypes(listOf(savingType("car", "Car"), savingType("house", "House")))
        sync.upsertSavings(
            listOf(
                saving("c1", "aug", "car", 1_000.0, date = Instant.parse("2026-08-05T00:00:00Z")),
                saving("c2", "sep", "car", 200.0, recurrent = true, date = Instant.parse("2026-09-05T00:00:00Z")),
                saving("c3", "sep", "car", 50.0, outcome = true, date = Instant.parse("2026-09-06T00:00:00Z")),
                saving("h1", "aug", "house", 300.0, date = Instant.parse("2026-08-01T00:00:00Z")),
            ),
        )

        val grouped = database.savingDao().groupedSavings().first()

        // Newest deposit first.
        assertEquals(listOf("Car", "House"), grouped.map { it.description })
        val car = grouped.first()
        assertEquals(1_150.0, car.totalGlobal, 0.0)
        assertEquals(150.0, car.totalLastMonth, 0.0)
        assertEquals(true, car.isRecurrentSaving)
        // The last deposit, not the later withdrawal.
        assertEquals(Instant.parse("2026-09-05T00:00:00Z"), car.lastSaving)
        assertEquals(0.0, grouped[1].totalLastMonth, 0.0)
    }

    @Test
    fun `the savings list shows deposits only`() = runTest {
        sync.upsertSavingTypes(listOf(savingType("car", "Car")))
        sync.upsertSavings(listOf(saving("in", "sep", "car", 10.0), saving("out", "sep", "car", 5.0, outcome = true)))

        val savings = database.savingDao().savings("sep").first()

        assertEquals(listOf("in"), savings.map { it.saving.uuid })
        assertEquals("Car", savings.single().savingTypeDescription)
    }
}
