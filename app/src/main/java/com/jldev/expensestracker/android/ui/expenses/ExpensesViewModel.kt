package com.jldev.expensestracker.android.ui.expenses

import androidx.lifecycle.viewModelScope
import com.jldev.expensestracker.android.data.CycleRepository
import com.jldev.expensestracker.android.data.ExpensesRepository
import com.jldev.expensestracker.android.data.model.BudgetTotal
import com.jldev.expensestracker.android.data.model.ExpenseItem
import com.jldev.expensestracker.android.data.sync.SyncRepository
import com.jldev.expensestracker.android.ui.UserMessages
import com.jldev.expensestracker.android.ui.common.CycleEntryListViewModel
import com.jldev.expensestracker.android.ui.components.EntryValues
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExpensesViewModel @Inject constructor(
    private val expensesRepository: ExpensesRepository,
    cycleRepository: CycleRepository,
    syncRepository: SyncRepository,
    userMessages: UserMessages,
) : CycleEntryListViewModel<ExpenseItem>(cycleRepository, syncRepository, userMessages) {

    private val _budgetFilter = MutableStateFlow<String?>(null)

    /** A budget uuid, [BudgetTotal.UNBUDGETED], or null for every expense. */
    val budgetFilter: StateFlow<String?> = _budgetFilter.asStateFlow()

    /** The shown cycle's budgets with their totals, including the synthetic "Sin ppto." entry. */
    val budgets: StateFlow<List<BudgetTotal>> = cycleRepository.currentCycle
        .map { it?.uuid }
        .distinctUntilChanged()
        .flatMapLatest { uuid -> if (uuid == null) flowOf(emptyList()) else expensesRepository.budgetTotals(uuid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // Another cycle has other budgets: clear the filter.
        viewModelScope.launch {
            cycleRepository.currentCycle.map { it?.uuid }.distinctUntilChanged().drop(1).collect {
                _budgetFilter.value = null
            }
        }
    }

    override fun items(cycleUuid: String): Flow<List<ExpenseItem>> =
        _budgetFilter.flatMapLatest { filter -> expensesRepository.expenses(cycleUuid, filter) }

    fun filterByBudget(budgetUuid: String?) {
        _budgetFilter.value = budgetUuid
    }

    fun save(values: EntryValues, budgetUuid: String?, createRecurrent: Boolean, createAnother: Boolean) {
        val isNew = uiState.value.editor?.item == null
        submit(
            successMessage = if (isNew) "Gasto registrado" else "Gasto actualizado",
            failureMessage = if (isNew) "No se pudo registrar el gasto" else "No se pudo actualizar el gasto",
            createAnother = createAnother,
        ) { item ->
            if (item == null) {
                expensesRepository.create(
                    description = values.description,
                    amount = values.amount,
                    date = Instant.now(),
                    cycleUuid = cycleForNewItems().uuid,
                    budgetUuid = budgetUuid,
                    createRecurrent = createRecurrent,
                )
            } else {
                expensesRepository.update(item.expense, values.description, values.amount, budgetUuid)
            }
        }
    }

    fun delete() = submit("Gasto eliminado", "No se pudo eliminar el gasto") { item ->
        expensesRepository.delete(checkNotNull(item).expense)
    }
}
