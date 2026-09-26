package com.jldev.expensestracker.android.ui.incomes

import com.jldev.expensestracker.android.data.IncomesRepository
import com.jldev.expensestracker.android.data.local.RecurrentIncomeEntity
import com.jldev.expensestracker.android.data.sync.SyncRepository
import com.jldev.expensestracker.android.ui.UserMessages
import com.jldev.expensestracker.android.ui.common.EntryListViewModel
import com.jldev.expensestracker.android.ui.components.EntryValues
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RecurrentIncomesViewModel @Inject constructor(
    private val incomesRepository: IncomesRepository,
    syncRepository: SyncRepository,
    userMessages: UserMessages,
) : EntryListViewModel<RecurrentIncomeEntity>(syncRepository, userMessages) {

    override fun items() = incomesRepository.recurrentIncomes()

    fun save(values: EntryValues, enabled: Boolean, createAnother: Boolean) {
        val isNew = uiState.value.editor?.item == null
        submit(
            successMessage = if (isNew) "Ingreso recurrente registrado" else "Ingreso recurrente actualizado",
            failureMessage = if (isNew) {
                "No se pudo registrar el ingreso recurrente"
            } else {
                "No se pudo actualizar el ingreso recurrente"
            },
            createAnother = createAnother,
        ) { item ->
            if (item == null) {
                incomesRepository.createRecurrent(values.description, values.amount)
            } else {
                incomesRepository.updateRecurrent(item, values.description, values.amount, enabled)
            }
        }
    }

    fun delete() = submit("Ingreso recurrente eliminado", "No se pudo eliminar el ingreso recurrente") { item ->
        incomesRepository.deleteRecurrent(checkNotNull(item))
    }
}
