@file:UseSerializers(InstantSerializer::class)

package com.jldev.expensestracker.android.data.remote.model

import com.jldev.expensestracker.android.data.remote.InstantSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import java.time.Instant

@Serializable
enum class ExpenseSource {
    @SerialName("App") App,
    @SerialName("Email") Email,
    @SerialName("Bot") Bot,
    @SerialName("Recurrent") Recurrent,
}

/** `ExpensePublic`. */
@Serializable
data class Expense(
    val id: Int,
    val description: String,
    val valExpense: Double,
    val dateExpense: Instant,
    val source: ExpenseSource = ExpenseSource.App,
    val categories: String = "",
    val budget: BudgetRef? = null,
    val cycle: Cycle,
    val isRecurrentExpense: Boolean,
)

/** `BudgetPublic`, embedded in an expense. */
@Serializable
data class BudgetRef(
    val id: Int,
    val description: String,
    val valBudget: Double,
)

// The backend's date_* defaults are evaluated once at server start, so the date is always sent.
@Serializable
data class ExpenseCreate(
    val description: String,
    val valExpense: Double,
    val dateExpense: Instant,
    val budgetId: Int? = null,
    /** Null targets the active cycle. */
    val cycleId: Int? = null,
    val createRecurrentExpense: Boolean = false,
)

/** PATCH body; null fields are left out and stay unchanged. */
@Serializable
data class ExpenseUpdate(
    val description: String? = null,
    val valExpense: Double? = null,
    val dateExpense: Instant? = null,
    /**
     * A budget id, or [JsonNull] to remove the budget (ApiJson drops Kotlin nulls, so clearing needs
     * a JSON null); see [budgetChange].
     */
    val budgetId: JsonElement? = null,
    val cycleId: Int? = null,
    val categories: String? = null,
)

/** [ExpenseUpdate.budgetId] that sets [budgetId], or removes the budget when it's null. */
fun budgetChange(budgetId: Int?): JsonElement = budgetId?.let(::JsonPrimitive) ?: JsonNull

@Serializable
data class RecurrentExpense(
    val id: Int,
    val description: String,
    val valExpense: Double,
    val enabled: Boolean = true,
    val categories: String = "",
    val userId: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class RecurrentExpenseCreate(
    val description: String,
    val valExpense: Double,
)

@Serializable
data class RecurrentExpenseUpdate(
    val description: String? = null,
    val valExpense: Double? = null,
    val enabled: Boolean? = null,
)
