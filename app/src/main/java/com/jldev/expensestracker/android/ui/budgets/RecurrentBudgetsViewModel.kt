package com.jldev.expensestracker.android.ui.budgets

import com.jldev.expensestracker.android.data.BudgetsRepository
import com.jldev.expensestracker.android.data.local.RecurrentBudgetEntity
import com.jldev.expensestracker.android.data.sync.SyncRepository
import com.jldev.expensestracker.android.ui.UserMessages
import com.jldev.expensestracker.android.ui.common.EntryListViewModel
import com.jldev.expensestracker.android.ui.components.EntryValues
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** The "Presupuestos" tab: budget templates copied into each new cycle (and into the current one). */
@HiltViewModel
class RecurrentBudgetsViewModel @Inject constructor(
    private val budgetsRepository: BudgetsRepository,
    syncRepository: SyncRepository,
    userMessages: UserMessages,
) : EntryListViewModel<RecurrentBudgetEntity>(syncRepository, userMessages) {

    override fun items() = budgetsRepository.recurrentBudgets()

    fun save(values: EntryValues, createAnother: Boolean) {
        val isNew = uiState.value.editor?.item == null
        submit(
            successMessage = if (isNew) "Presupuesto registrado" else "Presupuesto actualizado",
            failureMessage = if (isNew) "No se pudo registrar el presupuesto" else "No se pudo actualizar el presupuesto",
            createAnother = createAnother,
        ) { item ->
            if (item == null) {
                budgetsRepository.createRecurrent(values.description, values.amount)
            } else {
                budgetsRepository.updateRecurrent(item, values.description, values.amount)
            }
        }
    }

    fun delete() = submit("Presupuesto eliminado", "No se pudo eliminar el presupuesto") { item ->
        budgetsRepository.deleteRecurrent(checkNotNull(item))
    }
}
