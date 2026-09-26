package com.jldev.expensestracker.android.ui.savings

import com.jldev.expensestracker.android.data.CycleRepository
import com.jldev.expensestracker.android.data.SavingsRepository
import com.jldev.expensestracker.android.data.model.GroupedSaving
import com.jldev.expensestracker.android.data.sync.SyncRepository
import com.jldev.expensestracker.android.ui.UserMessages
import com.jldev.expensestracker.android.ui.common.CycleEntryListViewModel
import com.jldev.expensestracker.android.ui.components.EntryValues
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject

/**
 * The "Resumen" tab: totals per saving type, overall and for the active cycle. The list is read-only;
 * the add button creates a saving in the selected cycle, like the Ahorros tab.
 */
@HiltViewModel
class SavingsSummaryViewModel @Inject constructor(
    private val savingsRepository: SavingsRepository,
    cycleRepository: CycleRepository,
    syncRepository: SyncRepository,
    userMessages: UserMessages,
) : CycleEntryListViewModel<GroupedSaving>(cycleRepository, syncRepository, userMessages) {

    // The totals don't depend on the selected cycle; only new savings go to it.
    override fun items(cycleUuid: String): Flow<List<GroupedSaving>> = savingsRepository.groupedSavings()

    fun save(values: EntryValues, createRecurrent: Boolean, createAnother: Boolean) = submit(
        successMessage = "Ahorro registrado",
        failureMessage = "No se pudo registrar el ahorro",
        createAnother = createAnother,
    ) {
        savingsRepository.create(
            values.description, values.amount, Instant.now(), cycleForNewItems().uuid, createRecurrent,
        )
    }
}
