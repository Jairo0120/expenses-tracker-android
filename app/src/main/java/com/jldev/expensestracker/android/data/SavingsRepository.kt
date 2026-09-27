package com.jldev.expensestracker.android.data

import com.jldev.expensestracker.android.data.local.AppDatabase
import com.jldev.expensestracker.android.data.local.RecurrentSavingEntity
import com.jldev.expensestracker.android.data.local.SavingEntity
import com.jldev.expensestracker.android.data.local.SavingTypeEntity
import com.jldev.expensestracker.android.data.model.GroupedSaving
import com.jldev.expensestracker.android.data.model.RecurrentSavingItem
import com.jldev.expensestracker.android.data.model.SavingItem
import com.jldev.expensestracker.android.data.sync.LocalChange
import com.jldev.expensestracker.android.data.sync.SyncRepository
import com.jldev.expensestracker.android.data.sync.SyncTables
import com.jldev.expensestracker.android.data.sync.now
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Saving types are named like the backend does (Python's str.capitalize). */
fun savingTypeName(description: String): String = description.lowercase().replaceFirstChar { it.titlecase() }

/**
 * Savings and recurrent savings. A saving's "description" is its saving type's name: new names create
 * the type, and editing the description renames the type for all its savings (intended).
 */
@Singleton
class SavingsRepository @Inject constructor(
    database: AppDatabase,
    private val sync: SyncRepository,
) {
    private val savingDao = database.savingDao()

    fun savings(cycleUuid: String): Flow<List<SavingItem>> = savingDao.savings(cycleUuid)

    fun groupedSavings(): Flow<List<GroupedSaving>> = savingDao.groupedSavings()

    fun recurrentSavings(): Flow<List<RecurrentSavingItem>> = savingDao.recurrentSavings()

    /** "¿Ahorro mensual?" creates the type's recurrent saving if it has none (like the backend). */
    suspend fun create(description: String, amount: Double, date: Instant, cycleUuid: String, createRecurrent: Boolean) {
        val timestamp = now()
        val changes = mutableListOf<LocalChange>()
        val type = savingType(description, timestamp, changes)
        val recurrent = if (createRecurrent && savingDao.recurrentSavingOfType(type.uuid) == null) {
            RecurrentSavingEntity(
                uuid = newUuid(), createdAt = timestamp, updatedAt = timestamp,
                valSaving = amount, savingTypeUuid = type.uuid,
            ).also { changes += SyncTables.recurrentSavings.save(it) }
        } else {
            null
        }
        val saving = SavingEntity(
            uuid = newUuid(), createdAt = timestamp, updatedAt = timestamp,
            valSaving = amount, dateSaving = date, isRecurrentSaving = createRecurrent,
            cycleUuid = cycleUuid, savingTypeUuid = type.uuid,
            // Linked only to a recurrent saving created here: an existing one may have its copy already.
            recurrentSavingUuid = recurrent?.uuid,
        )
        changes += SyncTables.savings.save(saving)
        sync.save(changes)
    }

    suspend fun update(item: SavingItem, description: String, amount: Double) {
        val timestamp = now()
        val changes = renameType(item.saving.savingTypeUuid, item.savingTypeDescription, description, timestamp)
        changes += SyncTables.savings.save(item.saving.copy(valSaving = amount, updatedAt = timestamp))
        sync.save(changes)
    }

    suspend fun delete(item: SavingItem) {
        sync.save(listOf(SyncTables.savings.delete(item.saving.copy(updatedAt = now()))))
    }

    suspend fun createRecurrent(description: String, amount: Double) {
        val timestamp = now()
        val changes = mutableListOf<LocalChange>()
        val type = savingType(description, timestamp, changes)
        changes += SyncTables.recurrentSavings.save(
            RecurrentSavingEntity(
                uuid = newUuid(), createdAt = timestamp, updatedAt = timestamp,
                valSaving = amount, savingTypeUuid = type.uuid,
            ),
        )
        sync.save(changes)
    }

    suspend fun updateRecurrent(item: RecurrentSavingItem, description: String, amount: Double, enabled: Boolean) {
        val timestamp = now()
        val recurrent = item.recurrentSaving
        val changes = renameType(recurrent.savingTypeUuid, item.savingTypeDescription, description, timestamp)
        changes += SyncTables.recurrentSavings.save(
            recurrent.copy(valSaving = amount, enabled = enabled, updatedAt = timestamp),
        )
        sync.save(changes)
    }

    suspend fun deleteRecurrent(item: RecurrentSavingItem) {
        val recurrent = item.recurrentSaving
        sync.save(listOf(SyncTables.recurrentSavings.delete(recurrent.copy(updatedAt = now()))))
    }

    /** The saving type named [description], adding its creation to [changes] when it's new. */
    private suspend fun savingType(description: String, timestamp: Instant, changes: MutableList<LocalChange>): SavingTypeEntity {
        val name = savingTypeName(description)
        return savingDao.savingTypeNamed(name) ?: SavingTypeEntity(
            uuid = newUuid(), createdAt = timestamp, updatedAt = timestamp, description = name,
        ).also { changes += SyncTables.savingTypes.save(it) }
    }

    /** A rename of the saving type, when the description actually changed. */
    private suspend fun renameType(typeUuid: String, current: String, description: String, timestamp: Instant): MutableList<LocalChange> {
        if (description == current) return mutableListOf()
        val type = savingDao.savingType(typeUuid) ?: return mutableListOf()
        return mutableListOf(SyncTables.savingTypes.save(type.copy(description = description, updatedAt = timestamp)))
    }
}
