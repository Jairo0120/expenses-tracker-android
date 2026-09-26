package com.jldev.expensestracker.android.ui.expenses

import com.jldev.expensestracker.android.data.ExpensesRepository
import com.jldev.expensestracker.android.data.local.RecurrentExpenseEntity
import com.jldev.expensestracker.android.data.sync.SyncRepository
import com.jldev.expensestracker.android.ui.UserMessages
import com.jldev.expensestracker.android.ui.common.EntryListViewModel
import com.jldev.expensestracker.android.ui.components.EntryValues
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// Recurrent expenses are copied into each new cycle by the backend, so changes don't affect the current one.
@HiltViewModel
class RecurrentExpensesViewModel @Inject constructor(
    private val expensesRepository: ExpensesRepository,
    syncRepository: SyncRepository,
    userMessages: UserMessages,
) : EntryListViewModel<RecurrentExpenseEntity>(syncRepository, userMessages) {

    override fun items() = expensesRepository.recurrentExpenses()

    fun save(values: EntryValues, enabled: Boolean, createAnother: Boolean) {
        val isNew = uiState.value.editor?.item == null
        submit(
            successMessage = if (isNew) "Gasto registrado" else "Gasto actualizado",
            failureMessage = if (isNew) {
                "No se pudo registrar el gasto recurrente"
            } else {
                "No se pudo actualizar el gasto recurrente"
            },
            createAnother = createAnother,
        ) { item ->
            if (item == null) {
                expensesRepository.createRecurrent(values.description, values.amount)
            } else {
                expensesRepository.updateRecurrent(item, values.description, values.amount, enabled)
            }
        }
    }

    fun delete() = submit("Gasto eliminado", "No se pudo eliminar el gasto recurrente") { item ->
        expensesRepository.deleteRecurrent(checkNotNull(item))
    }
}
