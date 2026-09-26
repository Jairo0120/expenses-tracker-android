@file:UseSerializers(InstantSerializer::class)

package com.jldev.expensestracker.android.data.remote.model

import com.jldev.expensestracker.android.data.remote.InstantSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.Instant

/** `BudgetWithTotal`, from `GET /budgets`. */
@Serializable
data class Budget(
    val id: Int,
    val description: String,
    val valBudget: Double,
    val cycleId: Int,
    val totalSpent: Double,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    /** The backend adds a synthetic "Sin ppto." row with id 0 for expenses without a budget. */
    val isUnbudgeted: Boolean get() = id == UNBUDGETED_ID

    companion object {
        /** Also the `budget_id` filter value for expenses without a budget. */
        const val UNBUDGETED_ID = 0
    }
}

@Serializable
data class RecurrentBudget(
    val id: Int,
    val description: String,
    val valBudget: Double,
    val isEnabled: Boolean = true,
    val userId: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class RecurrentBudgetCreate(
    val description: String,
    val valBudget: Double,
)

@Serializable
data class RecurrentBudgetUpdate(
    val description: String? = null,
    val valBudget: Double? = null,
    val isEnabled: Boolean? = null,
)
