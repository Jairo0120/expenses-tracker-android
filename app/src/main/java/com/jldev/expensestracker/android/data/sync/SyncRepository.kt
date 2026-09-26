package com.jldev.expensestracker.android.data.sync

import androidx.room.withTransaction
import com.jldev.expensestracker.android.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/** A change the server refused; [reason] is the server's explanation. */
class SyncRejectedException(val reason: String?) : Exception("Change rejected: $reason")

/**
 * Keeps the local database in step with the server: pulls every change after the saved cursor, and
 * pushes the app's changes. Syncs never overlap.
 */
@Singleton
class SyncRepository @Inject constructor(
    private val api: SyncApi,
    private val database: AppDatabase,
    private val store: SyncStore,
) {
    private val mutex = Mutex()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    /** Pulls every change since the last sync. */
    suspend fun sync() = locked { pullAll() }

    /**
     * Sends [changes] in order, stores the server's copies, then pulls. Throws [SyncRejectedException]
     * when the server refused one (the others still count).
     */
    suspend fun push(changes: List<SyncChange>) = locked {
        val results = api.push(SyncPushRequest(changes)).results
        val dao = database.syncDao()
        database.withTransaction {
            changes.zip(results).forEach { (change, result) ->
                val table = SyncTables.byName.getValue(change.entity)
                val record = result.record
                if (record != null) {
                    table.store(dao, listOf(record))
                    // Merged into a record with another uuid: drop any copy under ours.
                    if (record["uuid"]?.jsonPrimitive?.content != change.uuid) table.remove(dao, listOf(change.uuid))
                }
            }
        }
        pullAll()
        results.firstOrNull { it.status == "rejected" }?.let { throw SyncRejectedException(it.reason) }
    }

    /** Forgets everything, e.g. on logout. */
    suspend fun clear() = mutex.withLock {
        withContext(Dispatchers.IO) { database.clearAllTables() }
        store.setCursor(0)
    }

    private suspend fun pullAll() {
        val dao = database.syncDao()
        var cursor = store.cursor()
        do {
            val page = api.pull(since = cursor)
            database.withTransaction {
                // Referenced tables first; references aren't enforced, so order is only for tidiness.
                SyncTables.all.forEach { table -> page.changes[table.name]?.let { table.store(dao, it) } }
            }
            // Saved after each page, so an interrupted first sync resumes where it stopped.
            cursor = page.cursor
            store.setCursor(cursor)
        } while (page.hasMore)
    }

    private suspend fun <R> locked(block: suspend () -> R): R = mutex.withLock {
        _isSyncing.value = true
        try {
            block()
        } finally {
            _isSyncing.value = false
        }
    }
}
