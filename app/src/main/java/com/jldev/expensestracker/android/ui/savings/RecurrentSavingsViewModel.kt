package com.jldev.expensestracker.android.ui.savings

import com.jldev.expensestracker.android.data.SavingsRepository
import com.jldev.expensestracker.android.data.model.RecurrentSavingItem
import com.jldev.expensestracker.android.data.sync.SyncRepository
import com.jldev.expensestracker.android.ui.UserMessages
import com.jldev.expensestracker.android.ui.common.EntryListViewModel
import com.jldev.expensestracker.android.ui.components.EntryValues
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RecurrentSavingsViewModel @Inject constructor(
    private val savingsRepository: SavingsRepository,
    syncRepository: SyncRepository,
    userMessages: UserMessages,
) : EntryListViewModel<RecurrentSavingItem>(syncRepository, userMessages) {

    override fun items() = savingsRepository.recurrentSavings()

    fun save(values: EntryValues, enabled: Boolean, createAnother: Boolean) {
        val isNew = uiState.value.editor?.item == null
        submit(
            successMessage = if (isNew) "Ahorro recurrente registrado" else "Ahorro recurrente actualizado",
            failureMessage = if (isNew) {
                "No se pudo registrar el ahorro recurrente"
            } else {
                "No se pudo actualizar el ahorro recurrente"
            },
            createAnother = createAnother,
        ) { item ->
            if (item == null) {
                savingsRepository.createRecurrent(values.description, values.amount)
            } else {
                savingsRepository.updateRecurrent(item, values.description, values.amount, enabled)
            }
        }
    }

    fun delete() = submit("Ahorro recurrente eliminado", "No se pudo eliminar el ahorro recurrente") { item ->
        savingsRepository.deleteRecurrent(checkNotNull(item))
    }
}
