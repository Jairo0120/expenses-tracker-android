package com.jldev.expensestracker.android.data.remote.api

import com.jldev.expensestracker.android.data.remote.model.Budget
import com.jldev.expensestracker.android.data.remote.model.RecurrentBudget
import com.jldev.expensestracker.android.data.remote.model.RecurrentBudgetCreate
import com.jldev.expensestracker.android.data.remote.model.RecurrentBudgetUpdate
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// A cycle's budgets are created by the backend from the recurrent budgets; they can't be created directly.
interface BudgetsApi {
    /** Null [cycleId] means the active cycle. Sorted by number of expenses, "Sin ppto." first. */
    @GET("budgets")
    suspend fun list(
        @Query("cycle_id") cycleId: Int? = null,
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = DEFAULT_PAGE_SIZE,
    ): List<Budget>

    @DELETE("budgets/{id}")
    suspend fun delete(@Path("id") id: Int)

    @GET("recurrent_budgets")
    suspend fun listRecurrent(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = DEFAULT_PAGE_SIZE,
    ): List<RecurrentBudget>

    @POST("recurrent_budgets")
    suspend fun createRecurrent(@Body body: RecurrentBudgetCreate): RecurrentBudget

    @PATCH("recurrent_budgets/{id}")
    suspend fun updateRecurrent(@Path("id") id: Int, @Body body: RecurrentBudgetUpdate): RecurrentBudget

    @DELETE("recurrent_budgets/{id}")
    suspend fun deleteRecurrent(@Path("id") id: Int)
}
