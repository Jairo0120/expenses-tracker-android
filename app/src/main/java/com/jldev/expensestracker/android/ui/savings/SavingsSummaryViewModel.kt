package com.jldev.expensestracker.android.ui.savings

import androidx.lifecycle.viewModelScope
import com.jldev.expensestracker.android.data.CycleRepository
import com.jldev.expensestracker.android.data.remote.api.SavingsApi
import com.jldev.expensestracker.android.data.remote.model.GroupedSaving
import com.jldev.expensestracker.android.ui.UserMessages
import com.jldev.expensestracker.android.ui.common.EntryListViewModel
import com.jldev.expensestracker.android.ui.common.runReporting
import com.jldev.expensestracker.android.ui.components.EntryValues
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The "Resumen" tab: totals per saving type, overall and for the active cycle. The list is read-only;
 * the add button creates a saving in the selected cycle, like the Ahorros tab.
 */
@HiltViewModel
class SavingsSummaryViewModel @Inject constructor(
    private val savingsApi: SavingsApi,
    private val cycleRepository: CycleRepository,
    userMessages: UserMessages,
) : EntryListViewModel<GroupedSaving>(userMessages, "Error al obtener el resumen de ahorros") {

    // The screen refreshes whenever it's shown, since the Ahorros tab changes these totals.

    /** The endpoint isn't paged and returns every saving type at once. */
    override suspend fun fetchPage(skip: Int, limit: Int): List<GroupedSaving> =
        if (skip == 0) savingsApi.listGrouped() else emptyList()

    override fun onChanged() {
        super.onChanged()
        viewModelScope.launch {
            userMessages.runReporting("Error al obtener el estado del ciclo") { cycleRepository.refreshStatus() }
        }
    }

    fun save(values: EntryValues, createRecurrent: Boolean, createAnother: Boolean) = submit(
        successMessage = "Ahorro registrado",
        failureMessage = "No se pudo registrar el ahorro",
        createAnother = createAnother,
    ) {
        savingsApi.create(newSaving(values, cycleRepository.selectedCycleId.value, createRecurrent))
    }
}
