@file:UseSerializers(InstantSerializer::class)

package com.jldev.expensestracker.android.data.sync

import com.jldev.expensestracker.android.data.remote.InstantSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.time.Instant
import java.time.LocalDate

/** The backend's sync protocol; see api/sync.py in expenses-tracker. */
interface SyncApi {
    /** [windowStart] limits budgets, expenses and incomes to cycles starting on or after it. */
    @GET("sync")
    suspend fun pull(
        @Query("since") since: Long,
        @Query("window_start") windowStart: LocalDate? = null,
        @Query("limit") limit: Int = PULL_PAGE_SIZE,
    ): SyncPullResponse

    @POST("sync")
    suspend fun push(@Body body: SyncPushRequest): SyncPushResponse

    /** An older cycle's budgets, expenses and incomes. */
    @GET("sync/cycles/{uuid}")
    suspend fun cycleRecords(@Path("uuid") cycleUuid: String): SyncCycleRecords

    companion object {
        const val PULL_PAGE_SIZE = 500

        /** The server accepts up to 500 changes per push. */
        const val PUSH_BATCH_SIZE = 500
    }
}

@Serializable
data class SyncPullResponse(
    /** Pass as `since` next time. */
    val cursor: Long,
    val hasMore: Boolean,
    /** Entity name (e.g. "expenses") → records, deleted ones with `deleted_at` set. */
    val changes: Map<String, JsonArray> = emptyMap(),
)

@Serializable
data class SyncCycleRecords(val changes: Map<String, JsonArray> = emptyMap())

@Serializable
data class SyncPushRequest(val changes: List<SyncChange>)

@Serializable
data class SyncChange(
    val entity: String,
    val uuid: String,
    /** When the change was made on the phone; the server keeps the latest edit. */
    val updatedAt: Instant,
    val deleted: Boolean = false,
    val data: JsonObject = JsonObject(emptyMap()),
)

@Serializable
data class SyncPushResponse(val results: List<SyncResult>)

@Serializable
data class SyncResult(
    val uuid: String,
    /** "applied", "stale", "merged" or "rejected". */
    val status: String,
    /** The server's copy, when there is one (for "merged", under the server's uuid). */
    val record: JsonObject? = null,
    val reason: String? = null,
)
