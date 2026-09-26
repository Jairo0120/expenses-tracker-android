package com.jldev.expensestracker.android.data

import com.jldev.expensestracker.android.data.local.BudgetEntity
import com.jldev.expensestracker.android.data.local.CycleEntity
import com.jldev.expensestracker.android.data.local.ExpenseEntity
import com.jldev.expensestracker.android.data.local.IncomeEntity
import com.jldev.expensestracker.android.data.local.SavingEntity
import com.jldev.expensestracker.android.data.local.SavingTypeEntity
import java.time.Instant
import java.time.LocalDate

// Compact builders for local records in tests.

val T0: Instant = Instant.parse("2026-09-01T12:00:00Z")

fun cycle(uuid: String, start: String, active: Boolean = false): CycleEntity {
    val startDate = LocalDate.parse(start)
    return CycleEntity(
        uuid = uuid, createdAt = T0, updatedAt = T0, description = uuid,
        startDate = startDate, endDate = startDate.plusMonths(1).minusDays(1), isActive = active,
    )
}

fun budget(uuid: String, cycle: String, amount: Double = 100.0) = BudgetEntity(
    uuid = uuid, createdAt = T0, updatedAt = T0, description = uuid, valBudget = amount, cycleUuid = cycle,
)

fun expense(
    uuid: String,
    cycle: String,
    amount: Double,
    budget: String? = null,
    recurrent: Boolean = false,
    date: Instant = T0,
) = ExpenseEntity(
    uuid = uuid, createdAt = T0, updatedAt = T0, description = uuid, valExpense = amount, dateExpense = date,
    isRecurrentExpense = recurrent, cycleUuid = cycle, budgetUuid = budget,
)

fun income(uuid: String, cycle: String, amount: Double) = IncomeEntity(
    uuid = uuid, createdAt = T0, updatedAt = T0, description = uuid, valIncome = amount, dateIncome = T0,
    cycleUuid = cycle,
)

fun savingType(uuid: String, name: String) =
    SavingTypeEntity(uuid = uuid, createdAt = T0, updatedAt = T0, description = name)

fun saving(
    uuid: String,
    cycle: String,
    type: String,
    amount: Double,
    outcome: Boolean = false,
    recurrent: Boolean = false,
    date: Instant = T0,
) = SavingEntity(
    uuid = uuid, createdAt = T0, updatedAt = T0, valSaving = amount, dateSaving = date,
    movementType = if (outcome) SavingEntity.MOVEMENT_OUTCOME else SavingEntity.MOVEMENT_INCOME,
    isRecurrentSaving = recurrent, cycleUuid = cycle, savingTypeUuid = type,
)
