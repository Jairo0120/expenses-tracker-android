package com.jldev.expensestracker.android.ui.common

import androidx.lifecycle.viewModelScope
import com.jldev.expensestracker.android.data.CycleRepository
import com.jldev.expensestracker.android.data.remote.model.CycleListItem
import com.jldev.expensestracker.android.ui.UserMessages
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * An [EntryListViewModel] whose items belong to the selected cycle and count towards its summary:
 * it reloads when another cycle is picked and refreshes the header after every change.
 * Subclasses call [start] from their init block (instead of refresh()).
 */
abstract class CycleEntryListViewModel<T : Any>(
    protected val cycleRepository: CycleRepository,
    userMessages: UserMessages,
    loadErrorMessage: String,
) : EntryListViewModel<T>(userMessages, loadErrorMessage) {

    val cycles: StateFlow<List<CycleListItem>> = cycleRepository.cycles
    val selectedCycleId: StateFlow<Int?> = cycleRepository.selectedCycleId

    /** Not done in init: loading calls into the subclass, which isn't constructed yet at that point. */
    protected fun start() {
        viewModelScope.launch {
            // Emits the current cycle first, which does the initial load.
            cycleRepository.selectedCycleId.collectLatest { onCycleChanged() }
        }
    }

    protected open suspend fun onCycleChanged() = refresh()

    override fun onChanged() {
        super.onChanged()
        viewModelScope.launch {
            userMessages.runReporting("Error al obtener el estado del ciclo") { cycleRepository.refreshStatus() }
        }
    }

    fun selectCycle(cycleId: Int) = cycleRepository.select(cycleId)
}
