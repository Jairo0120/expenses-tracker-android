package com.jldev.expensestracker.android.ui.savings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jldev.expensestracker.android.data.remote.model.RecurrentSaving
import com.jldev.expensestracker.android.ui.components.EnabledStatus
import com.jldev.expensestracker.android.ui.components.EntryCard
import com.jldev.expensestracker.android.ui.components.EntryForm
import com.jldev.expensestracker.android.ui.components.EntryFormDialog
import com.jldev.expensestracker.android.ui.components.EntryListScreen
import com.jldev.expensestracker.android.ui.components.LabeledSwitch
import com.jldev.expensestracker.android.ui.format.formatDate
import com.jldev.expensestracker.android.ui.theme.DodgerBlue

@Composable
fun RecurrentSavingsScreen(viewModel: RecurrentSavingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    EntryListScreen(
        state = uiState.list,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onAdd = viewModel::openNew,
        addDescription = "Nuevo ahorro recurrente",
        emptyText = "No hay ahorros recurrentes qué mostrar",
        endText = "No hay más ahorros recurrentes qué mostrar",
        key = RecurrentSaving::id,
    ) { item ->
        EntryCard(item.savingType.description, item.valSaving, onClick = { viewModel.openEdit(item) }) {
            Text(formatDate(item.createdAt), style = MaterialTheme.typography.labelSmall, color = DodgerBlue.c500)
            EnabledStatus(item.enabled)
        }
    }

    uiState.editor?.let { editor ->
        val item = editor.item
        val form = remember(editor) { EntryForm(item?.valSaving, item?.savingType?.description.orEmpty()) }
        var enabled by remember(editor) { mutableStateOf(item?.enabled ?: true) }
        EntryFormDialog(
            form = form,
            title = if (item == null) "Nuevo ahorro recurrente" else "Editar ahorro recurrente",
            amountLabel = "Total del ahorro",
            descriptionLabel = "Descripción del ahorro",
            isEditing = item != null,
            isSaving = uiState.isSaving,
            deleteQuestion = "¿Eliminar este ahorro recurrente?",
            onDismiss = viewModel::closeEditor,
            onSave = { values, createAnother -> viewModel.save(values, enabled, createAnother) },
            onDelete = viewModel::delete,
        ) {
            if (item != null) LabeledSwitch("¿Habilitado?", enabled, { enabled = it }, enabled = !uiState.isSaving)
        }
    }
}
