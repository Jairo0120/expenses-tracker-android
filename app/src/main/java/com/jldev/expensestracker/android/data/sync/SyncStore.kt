package com.jldev.expensestracker.android.data.sync

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.syncDataStore by preferencesDataStore(name = "sync")

/** Sync bookkeeping kept outside the database. */
@Singleton
class SyncStore @Inject constructor(@ApplicationContext private val context: Context) {
    private val cursorKey = longPreferencesKey("cursor")

    /** The last server version pulled; 0 before the first sync. */
    suspend fun cursor(): Long = context.syncDataStore.data.map { it[cursorKey] ?: 0L }.first()

    suspend fun setCursor(cursor: Long) {
        context.syncDataStore.edit { it[cursorKey] = cursor }
    }
}
