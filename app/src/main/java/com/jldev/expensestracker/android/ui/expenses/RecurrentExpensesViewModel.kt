package com.jldev.expensestracker.android.ui.expenses

import com.jldev.expensestracker.android.data.remote.api.ExpensesApi
import com.jldev.expensestracker.android.data.remote.model.RecurrentExpense
import com.jldev.expensestracker.android.data.remote.model.RecurrentExpenseCreate
import com.jldev.expensestracker.android.data.remote.model.RecurrentExpenseUpdate
import com.jldev.expensestracker.android.ui.UserMessages
import com.jldev.expensestracker.android.ui.common.EntryListViewModel
import com.jldev.expensestracker.android.ui.components.EntryValues
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// Recurrent expenses are copied into each new cycle by the backend, so changes don't affect the current one.
@HiltViewModel
class RecurrentExpensesViewModel @Inject constructor(
    private val expensesApi: ExpensesApi,
    userMessages: UserMessages,
) : EntryListViewModel<RecurrentExpense>(userMessages, "Error al obtener los gastos recurrentes") {

    init {
        refresh()
    }

    override suspend fun fetchPage(skip: Int, limit: Int) = expensesApi.listRecurrent(skip, limit)

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
                expensesApi.createRecurrent(RecurrentExpenseCreate(values.description, values.amount))
            } else {
                expensesApi.updateRecurrent(
                    item.id,
                    RecurrentExpenseUpdate(values.description, values.amount, enabled),
                )
            }
        }
    }

    fun delete() = submit("Gasto eliminado", "No se pudo eliminar el gasto recurrente") { item ->
        expensesApi.deleteRecurrent(checkNotNull(item).id)
    }
}
