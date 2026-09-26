@file:UseSerializers(InstantSerializer::class, LocalDateSerializer::class)

package com.jldev.expensestracker.android.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.jldev.expensestracker.android.data.remote.InstantSerializer
import com.jldev.expensestracker.android.data.remote.LocalDateSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.Instant
import java.time.LocalDate

// Each class is both a Room row and the record exchanged with GET/POST /sync (snake_case through ApiJson).
// References to other records are uuids, without enforced foreign keys: pull pages come in version order,
// so a record may arrive before one it references.

/** Fields every synced record has. `syncVersion` is null until the server has the record. */
interface SyncRecord {
    val uuid: String
    val syncVersion: Long?
    val createdAt: Instant
    val updatedAt: Instant

    /** Only set on records pulled as deleted; those are removed locally, never stored. */
    val deletedAt: Instant?
}

@Serializable
@Entity(tableName = "cycles")
data class CycleEntity(
    @PrimaryKey override val uuid: String,
    override val syncVersion: Long? = null,
    override val createdAt: Instant,
    override val updatedAt: Instant,
    override val deletedAt: Instant? = null,
    val description: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val isActive: Boolean = false,
) : SyncRecord

@Serializable
@Entity(tableName = "saving_types")
data class SavingTypeEntity(
    @PrimaryKey override val uuid: String,
    override val syncVersion: Long? = null,
    override val createdAt: Instant,
    override val updatedAt: Instant,
    override val deletedAt: Instant? = null,
    val description: String,
) : SyncRecord

@Serializable
@Entity(tableName = "recurrent_budgets")
data class RecurrentBudgetEntity(
    @PrimaryKey override val uuid: String,
    override val syncVersion: Long? = null,
    override val createdAt: Instant,
    override val updatedAt: Instant,
    override val deletedAt: Instant? = null,
    val description: String,
    val valBudget: Double,
    val isEnabled: Boolean = true,
) : SyncRecord

@Serializable
@Entity(tableName = "recurrent_expenses")
data class RecurrentExpenseEntity(
    @PrimaryKey override val uuid: String,
    override val syncVersion: Long? = null,
    override val createdAt: Instant,
    override val updatedAt: Instant,
    override val deletedAt: Instant? = null,
    val description: String,
    val valExpense: Double,
    val enabled: Boolean = true,
    val categories: String = "",
) : SyncRecord

@Serializable
@Entity(tableName = "recurrent_incomes")
data class RecurrentIncomeEntity(
    @PrimaryKey override val uuid: String,
    override val syncVersion: Long? = null,
    override val createdAt: Instant,
    override val updatedAt: Instant,
    override val deletedAt: Instant? = null,
    val description: String,
    val valIncome: Double,
    val enabled: Boolean = true,
) : SyncRecord

@Serializable
@Entity(tableName = "recurrent_savings")
data class RecurrentSavingEntity(
    @PrimaryKey override val uuid: String,
    override val syncVersion: Long? = null,
    override val createdAt: Instant,
    override val updatedAt: Instant,
    override val deletedAt: Instant? = null,
    val valSaving: Double,
    val enabled: Boolean = true,
    val savingTypeUuid: String,
) : SyncRecord

@Serializable
@Entity(tableName = "budgets", indices = [Index("cycleUuid")])
data class BudgetEntity(
    @PrimaryKey override val uuid: String,
    override val syncVersion: Long? = null,
    override val createdAt: Instant,
    override val updatedAt: Instant,
    override val deletedAt: Instant? = null,
    val description: String,
    val valBudget: Double,
    val cycleUuid: String,
    val recurrentBudgetUuid: String? = null,
) : SyncRecord

@Serializable
@Entity(tableName = "expenses", indices = [Index("cycleUuid"), Index("budgetUuid")])
data class ExpenseEntity(
    @PrimaryKey override val uuid: String,
    override val syncVersion: Long? = null,
    override val createdAt: Instant,
    override val updatedAt: Instant,
    override val deletedAt: Instant? = null,
    val description: String,
    val valExpense: Double,
    val dateExpense: Instant,
    /** "App", "Email", "Bot" or "Recurrent". */
    val source: String = "App",
    val categories: String = "",
    val isRecurrentExpense: Boolean = false,
    val cycleUuid: String,
    val budgetUuid: String? = null,
    val recurrentExpenseUuid: String? = null,
) : SyncRecord

@Serializable
@Entity(tableName = "incomes", indices = [Index("cycleUuid")])
data class IncomeEntity(
    @PrimaryKey override val uuid: String,
    override val syncVersion: Long? = null,
    override val createdAt: Instant,
    override val updatedAt: Instant,
    override val deletedAt: Instant? = null,
    val description: String,
    val valIncome: Double,
    val dateIncome: Instant,
    val isRecurrentIncome: Boolean = false,
    val cycleUuid: String,
    val recurrentIncomeUuid: String? = null,
) : SyncRecord

@Serializable
@Entity(tableName = "savings", indices = [Index("cycleUuid"), Index("savingTypeUuid")])
data class SavingEntity(
    @PrimaryKey override val uuid: String,
    override val syncVersion: Long? = null,
    override val createdAt: Instant,
    override val updatedAt: Instant,
    override val deletedAt: Instant? = null,
    val valSaving: Double,
    val dateSaving: Instant,
    /** "Income" (deposit) or "Outcome" (withdrawal). */
    val movementType: String = MOVEMENT_INCOME,
    val movementDescription: String = "",
    val isRecurrentSaving: Boolean = false,
    val cycleUuid: String,
    val savingTypeUuid: String,
    val recurrentSavingUuid: String? = null,
) : SyncRecord {
    companion object {
        const val MOVEMENT_INCOME = "Income"
        const val MOVEMENT_OUTCOME = "Outcome"
    }
}
