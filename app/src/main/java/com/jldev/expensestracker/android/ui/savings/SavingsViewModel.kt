package com.jldev.expensestracker.android.ui.savings

import com.jldev.expensestracker.android.data.CycleRepository
import com.jldev.expensestracker.android.data.SavingsRepository
import com.jldev.expensestracker.android.data.model.SavingItem
import com.jldev.expensestracker.android.data.sync.SyncRepository
import com.jldev.expensestracker.android.ui.UserMessages
import com.jldev.expensestracker.android.ui.common.CycleEntryListViewModel
import com.jldev.expensestracker.android.ui.components.EntryValues
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject

/** The "Ahorros" tab: deposits of the selected cycle. */
@HiltViewModel
class SavingsViewModel @Inject constructor(
    private val savingsRepository: SavingsRepository,
    cycleRepository: CycleRepository,
    syncRepository: SyncRepository,
    userMessages: UserMessages,
) : CycleEntryListViewModel<SavingItem>(cycleRepository, syncRepository, userMessages) {

    override fun items(cycleUuid: String) = savingsRepository.savings(cycleUuid)

    fun save(values: EntryValues, createRecurrent: Boolean, createAnother: Boolean) {
        val isNew = uiState.value.editor?.item == null
        submit(
            successMessage = if (isNew) "Ahorro registrado" else "Ahorro actualizado",
            failureMessage = if (isNew) "No se pudo registrar el ahorro" else "No se pudo actualizar el ahorro",
            createAnother = createAnother,
        ) { item ->
            if (item == null) {
                savingsRepository.create(
                    values.description, values.amount, Instant.now(), cycleForNewItems().uuid, createRecurrent,
                )
            } else {
                savingsRepository.update(item, values.description, values.amount)
            }
        }
    }

    fun delete() = submit("Ahorro eliminado", "No se pudo eliminar el ahorro") { item ->
        savingsRepository.delete(checkNotNull(item))
    }
}
