package com.jldev.expensestracker.android.ui.budgets

import com.jldev.expensestracker.android.data.remote.api.BudgetsApi
import com.jldev.expensestracker.android.data.remote.model.RecurrentBudget
import com.jldev.expensestracker.android.data.remote.model.RecurrentBudgetCreate
import com.jldev.expensestracker.android.data.remote.model.RecurrentBudgetUpdate
import com.jldev.expensestracker.android.ui.UserMessages
import com.jldev.expensestracker.android.ui.common.EntryListViewModel
import com.jldev.expensestracker.android.ui.components.EntryValues
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** The "Presupuestos" tab: budget templates the backend copies into each new cycle. */
@HiltViewModel
class RecurrentBudgetsViewModel @Inject constructor(
    private val budgetsApi: BudgetsApi,
    userMessages: UserMessages,
) : EntryListViewModel<RecurrentBudget>(userMessages, "Error al obtener los presupuestos") {

    init {
        refresh()
    }

    override suspend fun fetchPage(skip: Int, limit: Int) = budgetsApi.listRecurrent(skip, limit)

    fun save(values: EntryValues, createAnother: Boolean) {
        val isNew = uiState.value.editor?.item == null
        submit(
            successMessage = if (isNew) "Presupuesto registrado" else "Presupuesto actualizado",
            failureMessage = if (isNew) "No se pudo registrar el presupuesto" else "No se pudo actualizar el presupuesto",
            createAnother = createAnother,
        ) { item ->
            if (item == null) {
                budgetsApi.createRecurrent(RecurrentBudgetCreate(values.description, values.amount))
            } else {
                budgetsApi.updateRecurrent(item.id, RecurrentBudgetUpdate(values.description, values.amount))
            }
        }
    }

    fun delete() = submit("Presupuesto eliminado", "No se pudo eliminar el presupuesto") { item ->
        budgetsApi.deleteRecurrent(checkNotNull(item).id)
    }
}
