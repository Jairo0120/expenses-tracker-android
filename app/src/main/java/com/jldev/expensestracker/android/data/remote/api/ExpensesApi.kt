package com.jldev.expensestracker.android.data.remote.api

import com.jldev.expensestracker.android.data.remote.model.Expense
import com.jldev.expensestracker.android.data.remote.model.ExpenseCreate
import com.jldev.expensestracker.android.data.remote.model.ExpenseUpdate
import com.jldev.expensestracker.android.data.remote.model.RecurrentExpense
import com.jldev.expensestracker.android.data.remote.model.RecurrentExpenseCreate
import com.jldev.expensestracker.android.data.remote.model.RecurrentExpenseUpdate
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ExpensesApi {
    /** Null [cycleId] means the active cycle; [budgetId] `Budget.UNBUDGETED_ID` filters expenses without a budget. */
    @GET("expenses")
    suspend fun list(
        @Query("cycle_id") cycleId: Int? = null,
        @Query("budget_id") budgetId: Int? = null,
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = DEFAULT_PAGE_SIZE,
    ): List<Expense>

    @POST("expenses")
    suspend fun create(@Body body: ExpenseCreate): Expense

    @PATCH("expenses/{id}")
    suspend fun update(@Path("id") id: Int, @Body body: ExpenseUpdate): Expense

    @DELETE("expenses/{id}")
    suspend fun delete(@Path("id") id: Int)

    @GET("recurrent_expenses")
    suspend fun listRecurrent(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = DEFAULT_PAGE_SIZE,
    ): List<RecurrentExpense>

    @POST("recurrent_expenses")
    suspend fun createRecurrent(@Body body: RecurrentExpenseCreate): RecurrentExpense

    @PATCH("recurrent_expenses/{id}")
    suspend fun updateRecurrent(@Path("id") id: Int, @Body body: RecurrentExpenseUpdate): RecurrentExpense

    @DELETE("recurrent_expenses/{id}")
    suspend fun deleteRecurrent(@Path("id") id: Int)
}
