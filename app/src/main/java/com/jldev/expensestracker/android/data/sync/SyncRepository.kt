package com.jldev.expensestracker.android.data.sync

import androidx.room.withTransaction
import com.jldev.expensestracker.android.data.local.AppDatabase
import com.jldev.expensestracker.android.data.local.PendingChangeEntity
import com.jldev.expensestracker.android.data.remote.ApiJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** A local change the server refused; the local record was restored to the server's copy (or removed). */
data class SyncRejection(val entity: String, val reason: String?)

/**
 * Keeps the local database in step with the server. Changes made on the phone are applied locally at once
 * and queued ([save]); a sync pushes the queue in order, then pulls every change after the saved cursor.
 * Syncs never overlap.
 */
@Singleton
class SyncRepository @Inject constructor(
    private val api: SyncApi,
    private val database: AppDatabase,
    private val store: SyncStore,
    private val scheduler: SyncScheduler,
) {
    private val mutex = Mutex()
    private val syncDao = database.syncDao()
    private val pendingDao = database.pendingChangeDao()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    /** Local changes not yet accepted by the server. */
    val pendingCount: Flow<Int> = pendingDao.count()

    private val _rejections = MutableSharedFlow<SyncRejection>(extraBufferCapacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val rejections: SharedFlow<SyncRejection> = _rejections.asSharedFlow()

    /** Older cycles are pruned once per process, so a cycle opened on demand stays while it's viewed. */
    private var pruned = false

    /** Applies [changes] locally and queues them; a background sync sends them when there's a connection. */
    suspend fun save(changes: List<LocalChange>) {
        database.withTransaction {
            changes.forEach { local ->
                local.applyLocally(syncDao)
                val change = local.change
                pendingDao.insert(
                    PendingChangeEntity(
                        entity = change.entity,
                        uuid = change.uuid,
                        updatedAt = change.updatedAt,
                        deleted = change.deleted,
                        data = ApiJson.encodeToString(JsonObject.serializer(), change.data),
                    ),
                )
            }
        }
        scheduler.syncSoon()
    }

    /** Sends the queued changes, then pulls what changed on the server. */
    suspend fun sync() = locked {
        pushPending()
        pullAll()
        if (!pruned) {
            prune()
            pruned = true
        }
    }

    /** Fetches an older cycle's records (outside the sync window) into the local database. */
    suspend fun loadCycle(cycleUuid: String) {
        val records = api.cycleRecords(cycleUuid)
        database.withTransaction {
            val skip = pendingDao.uuids().toSet()
            SyncTables.all.forEach { table -> records.changes[table.name]?.let { table.store(syncDao, it, skip) } }
        }
    }

    /** Forgets everything, e.g. on logout. Queued changes are lost. */
    suspend fun clear() = mutex.withLock {
        withContext(Dispatchers.IO) { database.clearAllTables() }
        store.setCursor(0)
        pruned = false
    }

    private suspend fun pushPending() {
        while (true) {
            val batch = pendingDao.oldest(SyncApi.PUSH_BATCH_SIZE)
            if (batch.isEmpty()) return
            val results = api.push(SyncPushRequest(batch.map { it.toChange() })).results
            database.withTransaction {
                batch.zip(results).forEach { (pending, result) -> applyResult(pending, result) }
            }
        }
    }

    private suspend fun applyResult(pending: PendingChangeEntity, result: SyncResult) {
        pendingDao.delete(pending.id)
        val table = SyncTables.byName.getValue(pending.entity)
        val record = result.record
        val serverUuid = record?.get("uuid")?.jsonPrimitive?.content
        if (serverUuid != null && serverUuid != pending.uuid) {
            // Merged into a record the server already had: switch everything to its uuid.
            table.remove(syncDao, listOf(pending.uuid))
            syncDao.repoint(pending.uuid, serverUuid)
            pendingDao.replaceReferences(pending.uuid, serverUuid)
        }
        when {
            // A later local change to the same record is still queued: keep the local version.
            record != null && pendingDao.countFor(serverUuid!!) > 0 -> Unit
            record != null -> table.store(syncDao, listOf(record))
            // Refused and unknown to the server: the record only ever existed here.
            result.status == REJECTED -> table.remove(syncDao, listOf(pending.uuid))
        }
        if (result.status == REJECTED) _rejections.tryEmit(SyncRejection(pending.entity, result.reason))
    }

    private suspend fun pullAll() {
        var cursor = store.cursor()
        do {
            val page = api.pull(since = cursor, windowStart = windowStart())
            database.withTransaction {
                val skip = pendingDao.uuids().toSet()
                SyncTables.all.forEach { table -> page.changes[table.name]?.let { table.store(syncDao, it, skip) } }
            }
            // Saved after each page, so an interrupted first sync resumes where it stopped.
            cursor = page.cursor
            store.setCursor(cursor)
        } while (page.hasMore)
    }

    private suspend fun prune() {
        val start = windowStart().toString()
        database.withTransaction {
            syncDao.pruneBudgets(start)
            syncDao.pruneExpenses(start)
            syncDao.pruneIncomes(start)
        }
    }

    private suspend fun <R> locked(block: suspend () -> R): R = mutex.withLock {
        _isSyncing.value = true
        try {
            block()
        } finally {
            _isSyncing.value = false
        }
    }

    private fun PendingChangeEntity.toChange() = SyncChange(
        entity = entity,
        uuid = uuid,
        updatedAt = updatedAt,
        deleted = deleted,
        data = ApiJson.parseToJsonElement(data).jsonObject,
    )

    companion object {
        private const val REJECTED = "rejected"

        /** How many monthly cycles of expenses, incomes and budgets stay on the phone (savings: all). */
        const val WINDOW_MONTHS = 6L

        /** The first day of the oldest cycle kept on the phone. */
        fun windowStart(today: LocalDate = LocalDate.now()): LocalDate =
            today.withDayOfMonth(1).minusMonths(WINDOW_MONTHS - 1)
    }
}
