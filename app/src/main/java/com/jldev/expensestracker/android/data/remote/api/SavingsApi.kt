package com.jldev.expensestracker.android.data.remote.api

import com.jldev.expensestracker.android.data.remote.model.GroupedSaving
import com.jldev.expensestracker.android.data.remote.model.RecurrentSaving
import com.jldev.expensestracker.android.data.remote.model.RecurrentSavingCreate
import com.jldev.expensestracker.android.data.remote.model.RecurrentSavingUpdate
import com.jldev.expensestracker.android.data.remote.model.Saving
import com.jldev.expensestracker.android.data.remote.model.SavingCreate
import com.jldev.expensestracker.android.data.remote.model.SavingOutcomeCreate
import com.jldev.expensestracker.android.data.remote.model.SavingUpdate
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SavingsApi {
    /** Deposits only. Null [cycleId] means the active cycle. */
    @GET("savings")
    suspend fun list(
        @Query("cycle_id") cycleId: Int? = null,
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = DEFAULT_PAGE_SIZE,
    ): List<Saving>

    @GET("savings/grouped-savings")
    suspend fun listGrouped(): List<GroupedSaving>

    @POST("savings")
    suspend fun create(@Body body: SavingCreate): Saving

    @POST("savings/saving-outcome")
    suspend fun createOutcome(@Body body: SavingOutcomeCreate): Saving

    @PATCH("savings/{id}")
    suspend fun update(@Path("id") id: Int, @Body body: SavingUpdate): Saving

    @DELETE("savings/{id}")
    suspend fun delete(@Path("id") id: Int)

    @GET("recurrent_savings")
    suspend fun listRecurrent(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = DEFAULT_PAGE_SIZE,
    ): List<RecurrentSaving>

    @POST("recurrent_savings")
    suspend fun createRecurrent(@Body body: RecurrentSavingCreate): RecurrentSaving

    @PATCH("recurrent_savings/{id}")
    suspend fun updateRecurrent(@Path("id") id: Int, @Body body: RecurrentSavingUpdate): RecurrentSaving

    @DELETE("recurrent_savings/{id}")
    suspend fun deleteRecurrent(@Path("id") id: Int)
}
