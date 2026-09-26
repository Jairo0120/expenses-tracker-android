package com.jldev.expensestracker.android.data

import com.jldev.expensestracker.android.data.local.AppDatabase
import com.jldev.expensestracker.android.data.local.IncomeEntity
import com.jldev.expensestracker.android.data.local.RecurrentIncomeEntity
import com.jldev.expensestracker.android.data.sync.SyncRepository
import com.jldev.expensestracker.android.data.sync.SyncTables
import com.jldev.expensestracker.android.data.sync.now
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncomesRepository @Inject constructor(
    database: AppDatabase,
    private val sync: SyncRepository,
) {
    private val incomeDao = database.incomeDao()

    fun incomes(cycleUuid: String): Flow<List<IncomeEntity>> = incomeDao.incomes(cycleUuid)

    /** "¿Ingreso mensual?" also creates the recurrent income; this income is its copy for the cycle. */
    suspend fun create(description: String, amount: Double, date: Instant, cycleUuid: String, createRecurrent: Boolean) {
        val timestamp = now()
        val recurrent = if (createRecurrent) {
            RecurrentIncomeEntity(
                uuid = newUuid(), createdAt = timestamp, updatedAt = timestamp,
                description = description, valIncome = amount,
            )
        } else {
            null
        }
        val income = IncomeEntity(
            uuid = newUuid(), createdAt = timestamp, updatedAt = timestamp,
            description = description, valIncome = amount, dateIncome = date,
            isRecurrentIncome = createRecurrent, cycleUuid = cycleUuid, recurrentIncomeUuid = recurrent?.uuid,
        )
        sync.push(listOfNotNull(recurrent?.let { SyncTables.recurrentIncomes.change(it) }, SyncTables.incomes.change(income)))
    }

    suspend fun update(income: IncomeEntity, description: String, amount: Double) {
        val updated = income.copy(description = description, valIncome = amount, updatedAt = now())
        sync.push(listOf(SyncTables.incomes.change(updated)))
    }

    suspend fun delete(income: IncomeEntity) {
        sync.push(listOf(SyncTables.incomes.change(income.copy(updatedAt = now()), deleted = true)))
    }

    fun recurrentIncomes(): Flow<List<RecurrentIncomeEntity>> = incomeDao.recurrentIncomes()

    suspend fun createRecurrent(description: String, amount: Double) {
        val timestamp = now()
        val recurrent = RecurrentIncomeEntity(
            uuid = newUuid(), createdAt = timestamp, updatedAt = timestamp,
            description = description, valIncome = amount,
        )
        sync.push(listOf(SyncTables.recurrentIncomes.change(recurrent)))
    }

    suspend fun updateRecurrent(recurrent: RecurrentIncomeEntity, description: String, amount: Double, enabled: Boolean) {
        val updated = recurrent.copy(description = description, valIncome = amount, enabled = enabled, updatedAt = now())
        sync.push(listOf(SyncTables.recurrentIncomes.change(updated)))
    }

    suspend fun deleteRecurrent(recurrent: RecurrentIncomeEntity) {
        sync.push(listOf(SyncTables.recurrentIncomes.change(recurrent.copy(updatedAt = now()), deleted = true)))
    }
}
