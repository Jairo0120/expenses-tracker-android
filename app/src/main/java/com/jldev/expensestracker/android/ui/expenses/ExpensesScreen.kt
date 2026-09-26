package com.jldev.expensestracker.android.ui.expenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jldev.expensestracker.android.data.model.BudgetTotal
import com.jldev.expensestracker.android.data.model.ExpenseItem
import com.jldev.expensestracker.android.ui.common.EditorState
import com.jldev.expensestracker.android.ui.components.BudgetChips
import com.jldev.expensestracker.android.ui.components.CyclePicker
import com.jldev.expensestracker.android.ui.components.EntryCard
import com.jldev.expensestracker.android.ui.components.EntryForm
import com.jldev.expensestracker.android.ui.components.EntryFormDialog
import com.jldev.expensestracker.android.ui.components.EntryListScreen
import com.jldev.expensestracker.android.ui.components.EntryValues
import com.jldev.expensestracker.android.ui.components.LabeledSwitch
import com.jldev.expensestracker.android.ui.components.RecurrentMarker
import com.jldev.expensestracker.android.ui.format.formatDate
import com.jldev.expensestracker.android.ui.theme.DodgerBlue
import com.jldev.expensestracker.android.ui.theme.PersianRed

@Composable
fun ExpensesScreen(viewModel: ExpensesViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cycles by viewModel.cycles.collectAsStateWithLifecycle()
    val currentCycle by viewModel.currentCycle.collectAsStateWithLifecycle()
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val budgetFilter by viewModel.budgetFilter.collectAsStateWithLifecycle()

    EntryListScreen(
        state = uiState.list,
        onRefresh = viewModel::refresh,
        onAdd = viewModel::openNew,
        addDescription = "Nuevo gasto",
        emptyText = "No hay gastos qué mostrar",
        endText = "No hay más gastos qué mostrar",
        key = { it.expense.uuid },
        header = {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CyclePicker(
                    cycles = cycles,
                    selectedCycleUuid = currentCycle?.uuid,
                    onSelect = viewModel::selectCycle,
                )
                BudgetChips(
                    budgets = budgets,
                    selectedUuid = budgetFilter,
                    onSelect = viewModel::filterByBudget,
                    modifier = Modifier.weight(1f),
                )
            }
        },
    ) { expense ->
        ExpenseCard(expense, onClick = { viewModel.openEdit(expense) })
    }

    uiState.editor?.let { editor ->
        ExpenseFormDialog(
            editor = editor,
            // "Sin ppto." (id 0) is only a filter; the Expo form offered it and saved budget_id 0.
            budgets = budgets.filterNot(BudgetTotal::isUnbudgeted),
            isSaving = uiState.isSaving,
            onDismiss = viewModel::closeEditor,
            onSave = viewModel::save,
            onDelete = viewModel::delete,
        )
    }
}

@Composable
private fun ExpenseCard(item: ExpenseItem, onClick: () -> Unit) {
    val expense = item.expense
    EntryCard(
        description = expense.description,
        amount = expense.valExpense,
        onClick = onClick,
        middle = { if (expense.isRecurrentExpense) RecurrentMarker("Gasto recurrente") },
    ) {
        Text(
            item.budgetDescription ?: "Sin presupuesto",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (item.budgetDescription != null) DodgerBlue.c300 else PersianRed.c400,
        )
        Text(formatDate(expense.dateExpense), style = MaterialTheme.typography.labelSmall, color = DodgerBlue.c500)
    }
}

@Composable
private fun ExpenseFormDialog(
    editor: EditorState<ExpenseItem>,
    budgets: List<BudgetTotal>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (values: EntryValues, budgetUuid: String?, createRecurrent: Boolean, createAnother: Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    val expense = editor.item?.expense
    val form = remember(editor) { EntryForm(expense?.valExpense, expense?.description.orEmpty()) }
    var budgetUuid by remember(editor) { mutableStateOf(expense?.budgetUuid) }
    var createRecurrent by remember(editor) { mutableStateOf(false) }

    EntryFormDialog(
        form = form,
        title = if (expense == null) "Nuevo gasto" else "Editar gasto",
        amountLabel = "Total del gasto",
        descriptionLabel = "Descripción del gasto",
        isEditing = expense != null,
        isSaving = isSaving,
        deleteQuestion = "¿Eliminar este gasto?",
        onDismiss = onDismiss,
        onSave = { values, createAnother -> onSave(values, budgetUuid, createRecurrent, createAnother) },
        onDelete = onDelete,
    ) {
        Text("Presupuesto", fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White)
        BudgetChips(
            budgets = budgets,
            selectedUuid = budgetUuid,
            onSelect = { uuid ->
                budgetUuid = uuid
                budgets.firstOrNull { it.uuid == uuid }?.let { form.suggestDescription(it.description) }
            },
            enabled = !isSaving,
        )
        if (expense == null) {
            LabeledSwitch("¿Gasto mensual?", createRecurrent, { createRecurrent = it }, enabled = !isSaving)
        }
    }
}
