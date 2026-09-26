package com.jldev.expensestracker.android.ui.expenses

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jldev.expensestracker.android.data.local.RecurrentExpenseEntity
import com.jldev.expensestracker.android.ui.components.EnabledStatus
import com.jldev.expensestracker.android.ui.components.EntryCard
import com.jldev.expensestracker.android.ui.components.EntryForm
import com.jldev.expensestracker.android.ui.components.EntryFormDialog
import com.jldev.expensestracker.android.ui.components.EntryListScreen
import com.jldev.expensestracker.android.ui.components.LabeledSwitch
import com.jldev.expensestracker.android.ui.format.formatDate
import com.jldev.expensestracker.android.ui.theme.DodgerBlue

@Composable
fun RecurrentExpensesScreen(viewModel: RecurrentExpensesViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    EntryListScreen(
        state = uiState.list,
        onRefresh = viewModel::refresh,
        onAdd = viewModel::openNew,
        addDescription = "Nuevo gasto recurrente",
        emptyText = "No hay gastos recurrentes qué mostrar",
        endText = "No hay más gastos recurrentes qué mostrar",
        key = RecurrentExpenseEntity::uuid,
    ) { item ->
        EntryCard(item.description, item.valExpense, onClick = { viewModel.openEdit(item) }) {
            Text(formatDate(item.createdAt), style = MaterialTheme.typography.labelSmall, color = DodgerBlue.c500)
            EnabledStatus(item.enabled)
        }
    }

    uiState.editor?.let { editor ->
        val item = editor.item
        val form = remember(editor) { EntryForm(item?.valExpense, item?.description.orEmpty()) }
        var enabled by remember(editor) { mutableStateOf(item?.enabled ?: true) }
        EntryFormDialog(
            form = form,
            title = if (item == null) "Nuevo gasto recurrente" else "Editar gasto recurrente",
            amountLabel = "Total del gasto recurrente",
            descriptionLabel = "Descripción del gasto recurrente",
            isEditing = item != null,
            isSaving = uiState.isSaving,
            deleteQuestion = "¿Eliminar este gasto recurrente?",
            onDismiss = viewModel::closeEditor,
            onSave = { values, createAnother -> viewModel.save(values, enabled, createAnother) },
            onDelete = viewModel::delete,
        ) {
            if (item != null) LabeledSwitch("¿Habilitado?", enabled, { enabled = it }, enabled = !uiState.isSaving)
        }
    }
}
