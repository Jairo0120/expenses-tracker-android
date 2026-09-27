package com.jldev.expensestracker.android.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * A local change not yet accepted by the server, in the order it was made. Every change carries the
 * whole record, so they're never merged: pushing them in order is always safe (a later one wins).
 */
@Entity(tableName = "pending_changes")
data class PendingChangeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entity: String,
    val uuid: String,
    val updatedAt: Instant,
    val deleted: Boolean,
    /** The record's fields as sent in the push (JSON). */
    val data: String,
)

@Dao
interface PendingChangeDao {
    @Insert
    suspend fun insert(change: PendingChangeEntity)

    @Query("SELECT * FROM pending_changes ORDER BY id LIMIT :limit")
    suspend fun oldest(limit: Int): List<PendingChangeEntity>

    @Query("DELETE FROM pending_changes WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM pending_changes")
    fun count(): Flow<Int>

    @Query("SELECT COUNT(*) FROM pending_changes WHERE uuid = :uuid")
    suspend fun countFor(uuid: String): Int

    @Query("SELECT DISTINCT uuid FROM pending_changes")
    suspend fun uuids(): List<String>

    /** After a merge, queued changes must reference the server's uuid instead of ours. */
    @Query("UPDATE pending_changes SET data = REPLACE(data, :old, :new)")
    suspend fun replaceReferences(old: String, new: String)
}
