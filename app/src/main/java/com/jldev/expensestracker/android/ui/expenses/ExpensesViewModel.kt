package com.jldev.expensestracker.android.ui.expenses

import androidx.lifecycle.viewModelScope
import com.jldev.expensestracker.android.data.CycleRepository
import com.jldev.expensestracker.android.data.remote.api.BudgetsApi
import com.jldev.expensestracker.android.data.remote.api.ExpensesApi
import com.jldev.expensestracker.android.data.remote.model.Budget
import com.jldev.expensestracker.android.data.remote.model.CycleListItem
import com.jldev.expensestracker.android.data.remote.model.Expense
import com.jldev.expensestracker.android.data.remote.model.ExpenseCreate
import com.jldev.expensestracker.android.data.remote.model.ExpenseUpdate
import com.jldev.expensestracker.android.data.remote.model.budgetChange
import com.jldev.expensestracker.android.ui.UserMessages
import com.jldev.expensestracker.android.ui.common.CycleEntryListViewModel
import com.jldev.expensestracker.android.ui.common.runReporting
import com.jldev.expensestracker.android.ui.components.EntryValues
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class ExpenseFilters(
    val cycles: List<CycleListItem> = emptyList(),
    val selectedCycleId: Int? = null,
    /** The selected cycle's budgets with their totals, including the synthetic "Sin ppto." entry. */
    val budgets: List<Budget> = emptyList(),
    val budgetFilter: Int? = null,
)

@HiltViewModel
class ExpensesViewModel @Inject constructor(
    private val expensesApi: ExpensesApi,
    private val budgetsApi: BudgetsApi,
    cycleRepository: CycleRepository,
    userMessages: UserMessages,
) : CycleEntryListViewModel<Expense>(cycleRepository, userMessages, "Error al obtener los gastos") {

    private val budgets = MutableStateFlow<List<Budget>>(emptyList())
    private val budgetFilter = MutableStateFlow<Int?>(null)

    val filters: StateFlow<ExpenseFilters> = combine(
        cycleRepository.cycles,
        cycleRepository.selectedCycleId,
        budgets,
        budgetFilter,
        ::ExpenseFilters,
    ).stateIn(viewModelScope, SharingStarted.Eagerly, ExpenseFilters())

    init {
        start()
    }

    override suspend fun onCycleChanged() {
        budgetFilter.value = null
        refresh()
        loadBudgets()
    }

    override suspend fun fetchPage(skip: Int, limit: Int): List<Expense> =
        expensesApi.list(cycleRepository.selectedCycleId.value, budgetFilter.value, skip, limit)

    override fun onChanged() {
        super.onChanged()
        viewModelScope.launch { loadBudgets() }
    }

    fun filterByBudget(budgetId: Int?) {
        budgetFilter.value = budgetId
        refresh()
    }

    fun save(values: EntryValues, budgetId: Int?, createRecurrent: Boolean, createAnother: Boolean) {
        val isNew = uiState.value.editor?.item == null
        submit(
            successMessage = if (isNew) "Gasto registrado" else "Gasto actualizado",
            failureMessage = if (isNew) "No se pudo registrar el gasto" else "No se pudo actualizar el gasto",
            createAnother = createAnother,
        ) { expense ->
            if (expense == null) {
                expensesApi.create(
                    ExpenseCreate(
                        description = values.description,
                        valExpense = values.amount,
                        dateExpense = Instant.now(),
                        budgetId = budgetId,
                        cycleId = cycleRepository.selectedCycleId.value,
                        createRecurrentExpense = createRecurrent,
                    ),
                )
            } else {
                expensesApi.update(
                    expense.id,
                    ExpenseUpdate(
                        description = values.description,
                        valExpense = values.amount,
                        // Always sent, so deselecting the budget chip removes the budget.
                        budgetId = budgetChange(budgetId),
                    ),
                )
            }
        }
    }

    fun delete() = submit("Gasto eliminado", "No se pudo eliminar el gasto") { expense ->
        expensesApi.delete(checkNotNull(expense).id)
    }

    private suspend fun loadBudgets() {
        userMessages.runReporting("No se pudieron cargar los presupuestos") {
            budgets.value = budgetsApi.list(cycleRepository.selectedCycleId.value, limit = BUDGETS_LIMIT)
        }
    }

    private companion object {
        /** All of a cycle's budgets fit in one request (the backend default, as the Expo app used). */
        const val BUDGETS_LIMIT = 100
    }
}
