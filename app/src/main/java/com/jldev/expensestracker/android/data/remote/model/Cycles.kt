@file:UseSerializers(LocalDateSerializer::class)

package com.jldev.expensestracker.android.data.remote.model

import com.jldev.expensestracker.android.data.remote.LocalDateSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.LocalDate

/** `CyclePublic`, embedded in expenses and savings. */
@Serializable
data class Cycle(
    val id: Int,
    val description: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val isActive: Boolean = true,
)

/** `CycleSimpleList`, from `GET /cycles/list-cycles` (newest first). */
@Serializable
data class CycleListItem(
    val id: Int,
    val description: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
)

/** `CycleExpensesStatus`, from `GET /cycles/cycle-status`. */
@Serializable
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
