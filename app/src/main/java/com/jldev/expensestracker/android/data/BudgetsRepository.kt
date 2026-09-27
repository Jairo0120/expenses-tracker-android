package com.jldev.expensestracker.android.data

import com.jldev.expensestracker.android.data.local.AppDatabase
import com.jldev.expensestracker.android.data.local.BudgetEntity
import com.jldev.expensestracker.android.data.local.CycleEntity
import com.jldev.expensestracker.android.data.local.RecurrentBudgetEntity
import com.jldev.expensestracker.android.data.sync.LocalChange
import com.jldev.expensestracker.android.data.sync.SyncRepository
import com.jldev.expensestracker.android.data.sync.SyncTables
import com.jldev.expensestracker.android.data.sync.now
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Recurrent budgets (the "Presupuestos" tab). Like the backend's REST endpoints did, changes also apply
 * to the active cycle's copy: creating one adds the budget to the current month, editing or deleting one
 * edits or deletes that budget.
 */
@Singleton
class BudgetsRepository @Inject constructor(
    database: AppDatabase,
    private val cycles: CycleRepository,
    private val sync: SyncRepository,
) {
    private val budgetDao = database.budgetDao()

    fun recurrentBudgets(): Flow<List<RecurrentBudgetEntity>> = budgetDao.recurrentBudgets()

    suspend fun createRecurrent(description: String, amount: Double) {
        val timestamp = now()
        val recurrent = RecurrentBudgetEntity(
            uuid = newUuid(), createdAt = timestamp, updatedAt = timestamp,
            description = description, valBudget = amount,
        )
        val copy = cycles.active()?.let { cycle ->
            BudgetEntity(
                uuid = newUuid(), createdAt = timestamp, updatedAt = timestamp,
                description = description, valBudget = amount, cycleUuid = cycle.uuid,
                recurrentBudgetUuid = recurrent.uuid,
            )
        }
        sync.save(
            listOfNotNull(
                SyncTables.recurrentBudgets.save(recurrent),
                copy?.let { SyncTables.budgets.save(it) },
            ),
        )
    }

    suspend fun updateRecurrent(recurrent: RecurrentBudgetEntity, description: String, amount: Double) {
        val timestamp = now()
        val updated = recurrent.copy(description = description, valBudget = amount, updatedAt = timestamp)
        val copy = currentCopy(recurrent)?.let {
            it.copy(description = description, valBudget = amount, updatedAt = timestamp)
        }
        sync.save(listOfNotNull(SyncTables.recurrentBudgets.save(updated), copy?.let { SyncTables.budgets.save(it) }))
    }

    suspend fun deleteRecurrent(recurrent: RecurrentBudgetEntity) {
        val timestamp = now()
        val changes = mutableListOf<LocalChange>(
            SyncTables.recurrentBudgets.delete(recurrent.copy(updatedAt = timestamp)),
        )
        currentCopy(recurrent)?.let { changes += SyncTables.budgets.delete(it.copy(updatedAt = timestamp)) }
        sync.save(changes)
    }

    private suspend fun currentCopy(recurrent: RecurrentBudgetEntity): BudgetEntity? {
        val cycle: CycleEntity = cycles.active() ?: return null
        return budgetDao.copyOf(recurrent.uuid, cycle.uuid)
            ?: budgetDao.unlinkedByDescription(recurrent.description, cycle.uuid)
    }
}
