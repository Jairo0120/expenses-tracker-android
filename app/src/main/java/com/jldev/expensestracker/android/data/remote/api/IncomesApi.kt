package com.jldev.expensestracker.android.data.remote.api

import com.jldev.expensestracker.android.data.remote.model.Income
import com.jldev.expensestracker.android.data.remote.model.IncomeCreate
import com.jldev.expensestracker.android.data.remote.model.IncomeUpdate
import com.jldev.expensestracker.android.data.remote.model.RecurrentIncome
import com.jldev.expensestracker.android.data.remote.model.RecurrentIncomeCreate
import com.jldev.expensestracker.android.data.remote.model.RecurrentIncomeUpdate
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface IncomesApi {
    /** Null [cycleId] means the active cycle. */
    @GET("incomes")
    suspend fun list(
        @Query("cycle_id") cycleId: Int? = null,
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = DEFAULT_PAGE_SIZE,
    ): List<Income>

    @POST("incomes")
    suspend fun create(@Body body: IncomeCreate): Income

    @PATCH("incomes/{id}")
    suspend fun update(@Path("id") id: Int, @Body body: IncomeUpdate): Income

    @DELETE("incomes/{id}")
    suspend fun delete(@Path("id") id: Int)

    @GET("recurrent_incomes")
    suspend fun listRecurrent(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = DEFAULT_PAGE_SIZE,
    ): List<RecurrentIncome>

    @POST("recurrent_incomes")
    suspend fun createRecurrent(@Body body: RecurrentIncomeCreate): RecurrentIncome

    @PATCH("recurrent_incomes/{id}")
    suspend fun updateRecurrent(@Path("id") id: Int, @Body body: RecurrentIncomeUpdate): RecurrentIncome

    @DELETE("recurrent_incomes/{id}")
    suspend fun deleteRecurrent(@Path("id") id: Int)
}
