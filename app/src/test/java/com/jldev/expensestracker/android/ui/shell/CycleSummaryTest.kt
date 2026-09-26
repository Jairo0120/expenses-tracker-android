package com.jldev.expensestracker.android.ui.shell

import com.jldev.expensestracker.android.data.remote.model.CycleStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class CycleSummaryTest {

    // moneyAvailable = 1000 (incomes 1500 - recurrent 300 - savings 200), so spent is the only variable.
    private fun statusSpending(spent: Double) = CycleStatus(
        totalRecurrentExpenses = 300.0, totalExpenses = spent, totalIncomes = 1_500.0, totalSavings = 200.0,
    )

    @Test
    fun `level follows the Expo header thresholds`() {
        assertEquals(AvailableLevel.High, availableLevel(statusSpending(200.0))) // 80% left
        assertEquals(AvailableLevel.Medium, availableLevel(statusSpending(400.0))) // 60% left
        assertEquals(AvailableLevel.Low, availableLevel(statusSpending(700.0))) // 30% left
        assertEquals(AvailableLevel.VeryLow, availableLevel(statusSpending(950.0))) // 5% left
        assertEquals(AvailableLevel.Exhausted, availableLevel(statusSpending(995.0))) // 0.5% left
        assertEquals(AvailableLevel.Exhausted, availableLevel(statusSpending(1_200.0))) // overspent
    }

    @Test
    fun `boundaries are exclusive like the Expo comparisons`() {
        assertEquals(AvailableLevel.Medium, availableLevel(statusSpending(300.0))) // exactly 70% left
        assertEquals(AvailableLevel.Low, availableLevel(statusSpending(500.0))) // exactly 50% left
    }
}
