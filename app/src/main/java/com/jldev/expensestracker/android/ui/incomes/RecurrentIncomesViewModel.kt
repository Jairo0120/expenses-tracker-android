package com.jldev.expensestracker.android.ui.incomes

import com.jldev.expensestracker.android.data.remote.api.IncomesApi
import com.jldev.expensestracker.android.data.remote.model.RecurrentIncome
import com.jldev.expensestracker.android.data.remote.model.RecurrentIncomeCreate
import com.jldev.expensestracker.android.data.remote.model.RecurrentIncomeUpdate
import com.jldev.expensestracker.android.ui.UserMessages
import com.jldev.expensestracker.android.ui.common.EntryListViewModel
import com.jldev.expensestracker.android.ui.components.EntryValues
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RecurrentIncomesViewModel @Inject constructor(
    private val incomesApi: IncomesApi,
    userMessages: UserMessages,
) : EntryListViewModel<RecurrentIncome>(userMessages, "Error al obtener los ingresos recurrentes") {

    init {
        refresh()
    }

    override suspend fun fetchPage(skip: Int, limit: Int) = incomesApi.listRecurrent(skip, limit)

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
                incomesApi.createRecurrent(RecurrentIncomeCreate(values.description, values.amount))
            } else {
                incomesApi.updateRecurrent(item.id, RecurrentIncomeUpdate(values.description, values.amount, enabled))
            }
        }
    }

    fun delete() = submit("Ingreso recurrente eliminado", "No se pudo eliminar el ingreso recurrente") { item ->
        incomesApi.deleteRecurrent(checkNotNull(item).id)
    }
}
