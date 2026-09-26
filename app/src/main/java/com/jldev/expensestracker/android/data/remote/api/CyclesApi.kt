package com.jldev.expensestracker.android.data.remote.api

import com.jldev.expensestracker.android.data.remote.model.CycleListItem
import com.jldev.expensestracker.android.data.remote.model.CycleStatus
import retrofit2.http.GET
import retrofit2.http.Query

interface CyclesApi {
    /** Null [cycleId] means the active cycle. */
    @GET("cycles/cycle-status")
    suspend fun getStatus(@Query("cycle_id") cycleId: Int? = null): CycleStatus

    @GET("cycles/list-cycles")
    suspend fun list(): List<CycleListItem>
}
