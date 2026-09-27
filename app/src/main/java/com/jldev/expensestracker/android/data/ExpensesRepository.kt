package com.jldev.expensestracker.android.data

import com.jldev.expensestracker.android.data.local.AppDatabase
import com.jldev.expensestracker.android.data.local.ExpenseEntity
import com.jldev.expensestracker.android.data.local.RecurrentExpenseEntity
import com.jldev.expensestracker.android.data.model.BudgetTotal
import com.jldev.expensestracker.android.data.model.ExpenseItem
import com.jldev.expensestracker.android.data.sync.SyncRepository
import com.jldev.expensestracker.android.data.sync.SyncTables
import com.jldev.expensestracker.android.data.sync.now
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpensesRepository @Inject constructor(
    database: AppDatabase,
    private val sync: SyncRepository,
) {
    private val expenseDao = database.expenseDao()
    private val budgetDao = database.budgetDao()

    /** A cycle's expenses; [budgetFilter] is a budget uuid, [BudgetTotal.UNBUDGETED], or null for all. */
    fun expenses(cycleUuid: String, budgetFilter: String?): Flow<List<ExpenseItem>> = expenseDao.expenses(
        cycleUuid,
        budgetUuid = budgetFilter?.takeUnless { it == BudgetTotal.UNBUDGETED },
        unbudgetedOnly = budgetFilter == BudgetTotal.UNBUDGETED,
    )

    /** Like GET /budgets: "Sin ppto." first when there are expenses without a budget. */
    fun budgetTotals(cycleUuid: String): Flow<List<BudgetTotal>> =
        combine(budgetDao.budgetTotals(cycleUuid), budgetDao.unbudgetedTotal(cycleUuid)) { budgets, unbudgeted ->
            if (unbudgeted > 0) {
                listOf(BudgetTotal(BudgetTotal.UNBUDGETED, "Sin ppto.", 0.0, unbudgeted)) + budgets
            } else {
                budgets
            }
        }

    /** "¿Gasto mensual?" also creates the recurrent expense; this expense is its copy for the cycle. */
    suspend fun create(
        description: String,
        amount: Double,
        date: Instant,
        cycleUuid: String,
        budgetUuid: String?,
        createRecurrent: Boolean,
    ) {
        val timestamp = now()
        val recurrent = if (createRecurrent) {
            RecurrentExpenseEntity(
                uuid = newUuid(), createdAt = timestamp, updatedAt = timestamp,
                description = description, valExpense = amount,
            )
        } else {
            null
        }
        val expense = ExpenseEntity(
            uuid = newUuid(), createdAt = timestamp, updatedAt = timestamp,
            description = description, valExpense = amount, dateExpense = date,
            isRecurrentExpense = createRecurrent, cycleUuid = cycleUuid, budgetUuid = budgetUuid,
            recurrentExpenseUuid = recurrent?.uuid,
        )
        sync.save(
            listOfNotNull(
                recurrent?.let { SyncTables.recurrentExpenses.save(it) },
                SyncTables.expenses.save(expense),
            ),
        )
    }

    /** A null [budgetUuid] removes the budget. */
    suspend fun update(expense: ExpenseEntity, description: String, amount: Double, budgetUuid: String?) {
        val updated = expense.copy(
            description = description, valExpense = amount, budgetUuid = budgetUuid, updatedAt = now(),
        )
        sync.save(listOf(SyncTables.expenses.save(updated)))
    }

    suspend fun delete(expense: ExpenseEntity) {
        sync.save(listOf(SyncTables.expenses.delete(expense.copy(updatedAt = now()))))
    }

    fun recurrentExpenses(): Flow<List<RecurrentExpenseEntity>> = expenseDao.recurrentExpenses()

    suspend fun createRecurrent(description: String, amount: Double) {
        val timestamp = now()
        val recurrent = RecurrentExpenseEntity(
            uuid = newUuid(), createdAt = timestamp, updatedAt = timestamp,
            description = description, valExpense = amount,
        )
        sync.save(listOf(SyncTables.recurrentExpenses.save(recurrent)))
    }

    suspend fun updateRecurrent(recurrent: RecurrentExpenseEntity, description: String, amount: Double, enabled: Boolean) {
        val updated = recurrent.copy(description = description, valExpense = amount, enabled = enabled, updatedAt = now())
        sync.save(listOf(SyncTables.recurrentExpenses.save(updated)))
    }

    suspend fun deleteRecurrent(recurrent: RecurrentExpenseEntity) {
        sync.save(listOf(SyncTables.recurrentExpenses.delete(recurrent.copy(updatedAt = now()))))
    }
}

/** Records created on the phone get their permanent id right away. */
fun newUuid(): String = UUID.randomUUID().toString()
