package com.jldev.expensestracker.android.data

import com.jldev.expensestracker.android.data.remote.api.CyclesApi
import com.jldev.expensestracker.android.data.remote.model.CycleListItem
import com.jldev.expensestracker.android.data.remote.model.CycleStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-wide cycle state, replacing the Expo app's CycleContext, CycleListContext and ExpenseSummaryContext.
 * Screens call [refreshStatus] after changing expenses, incomes or savings so the header stays current.
 */
@Singleton
class CycleRepository @Inject constructor(
    private val cyclesApi: CyclesApi,
) {
    private val _cycles = MutableStateFlow<List<CycleListItem>>(emptyList())
    val cycles: StateFlow<List<CycleListItem>> = _cycles.asStateFlow()

    private val _selectedCycleId = MutableStateFlow<Int?>(null)

    /** Null means the backend's active cycle, which every endpoint defaults to. */
    val selectedCycleId: StateFlow<Int?> = _selectedCycleId.asStateFlow()

    private val _status = MutableStateFlow<CycleStatus?>(null)
    val status: StateFlow<CycleStatus?> = _status.asStateFlow()

    fun select(cycleId: Int?) {
        _selectedCycleId.value = cycleId
    }

    suspend fun loadCycles() {
        _cycles.value = cyclesApi.list()
    }

    suspend fun refreshStatus() {
        val cycleId = _selectedCycleId.value
        val status = cyclesApi.getStatus(cycleId)
        // Drop the result if the user picked another cycle meanwhile.
        if (_selectedCycleId.value == cycleId) _status.value = status
    }

    /** Forgets everything, e.g. on logout, so the next user doesn't see stale data. */
    fun clear() {
        _cycles.value = emptyList()
        _selectedCycleId.value = null
        _status.value = null
    }
}
