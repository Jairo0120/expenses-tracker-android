package com.jldev.expensestracker.android.data.model

import androidx.room.Embedded
import com.jldev.expensestracker.android.data.local.ExpenseEntity
import com.jldev.expensestracker.android.data.local.RecurrentSavingEntity
import com.jldev.expensestracker.android.data.local.SavingEntity
import java.time.Instant

// What screens show, computed from the local database with the same rules as the backend's queries.

data class ExpenseItem(
    @Embedded val expense: ExpenseEntity,
    val budgetDescription: String?,
)

data class SavingItem(
    @Embedded val saving: SavingEntity,
    val savingTypeDescription: String,
)

data class RecurrentSavingItem(
    @Embedded val recurrentSaving: RecurrentSavingEntity,
    val savingTypeDescription: String,
)

/** A cycle's budget with what was spent on it; see [UNBUDGETED]. */
data class BudgetTotal(
    val uuid: String,
    val description: String,
    val valBudget: Double,
    val totalSpent: Double,
) {
    val isUnbudgeted: Boolean get() = uuid == UNBUDGETED

    companion object {
        /** The synthetic "Sin ppto." entry for expenses without a budget: a list filter, not a record. */
        const val UNBUDGETED = "sin-ppto"
    }
}

/** A cycle's totals, like the backend's cycle-status. */
data class CycleStatus(
    val totalRecurrentExpenses: Double,
    val totalExpenses: Double,
    val totalIncomes: Double,
    val totalSavings: Double,
) {
    // Same derivation as getCycleStatus() in the Expo app's api/cycles.js.
    val spent: Double
        get() = if (totalIncomes == 0.0) totalRecurrentExpenses + totalExpenses else totalExpenses

    val moneyAvailable: Double
        get() = if (totalIncomes == 0.0) 0.0 else totalIncomes - (totalRecurrentExpenses + totalSavings)
}

/** Totals per saving type, like the backend's grouped-savings. */
data class GroupedSaving(
    val uuid: String,
    val description: String,
    val isRecurrentSaving: Boolean,
    /** Every movement ever: deposits minus withdrawals. */
    val totalGlobal: Double,
    /** Movements in the active cycle. */
    val totalLastMonth: Double,
    val lastSaving: Instant?,
)
