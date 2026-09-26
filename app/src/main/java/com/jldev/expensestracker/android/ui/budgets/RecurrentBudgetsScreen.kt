package com.jldev.expensestracker.android.ui.budgets

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jldev.expensestracker.android.data.local.RecurrentBudgetEntity
import com.jldev.expensestracker.android.ui.components.EntryCard
import com.jldev.expensestracker.android.ui.components.EntryForm
import com.jldev.expensestracker.android.ui.components.EntryFormDialog
import com.jldev.expensestracker.android.ui.components.EntryListScreen
import com.jldev.expensestracker.android.ui.format.formatDate
import com.jldev.expensestracker.android.ui.theme.DodgerBlue

@Composable
fun RecurrentBudgetsScreen(viewModel: RecurrentBudgetsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    EntryListScreen(
        state = uiState.list,
        onRefresh = viewModel::refresh,
        onAdd = viewModel::openNew,
        addDescription = "Nuevo presupuesto",
        emptyText = "No hay presupuestos qué mostrar",
        endText = "No hay más presupuestos qué mostrar",
        key = RecurrentBudgetEntity::uuid,
    ) { item ->
        EntryCard(item.description, item.valBudget, onClick = { viewModel.openEdit(item) }) {
            Text(formatDate(item.createdAt), style = MaterialTheme.typography.labelSmall, color = DodgerBlue.c500)
        }
    }

    uiState.editor?.let { editor ->
        val item = editor.item
        val form = remember(editor) { EntryForm(item?.valBudget, item?.description.orEmpty()) }
        EntryFormDialog(
            form = form,
            title = if (item == null) "Nuevo presupuesto" else "Editar presupuesto",
            amountLabel = "Total del presupuesto",
            descriptionLabel = "Descripción del presupuesto",
            isEditing = item != null,
            isSaving = uiState.isSaving,
            deleteQuestion = "¿Eliminar este presupuesto?",
            onDismiss = viewModel::closeEditor,
            onSave = viewModel::save,
            onDelete = viewModel::delete,
        )
    }
}
