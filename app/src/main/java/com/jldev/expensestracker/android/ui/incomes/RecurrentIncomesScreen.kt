package com.jldev.expensestracker.android.ui.incomes

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jldev.expensestracker.android.data.local.RecurrentIncomeEntity
import com.jldev.expensestracker.android.ui.components.EnabledStatus
import com.jldev.expensestracker.android.ui.components.EntryCard
import com.jldev.expensestracker.android.ui.components.EntryForm
import com.jldev.expensestracker.android.ui.components.EntryFormDialog
import com.jldev.expensestracker.android.ui.components.EntryListScreen
import com.jldev.expensestracker.android.ui.components.LabeledSwitch
import com.jldev.expensestracker.android.ui.format.formatDate
import com.jldev.expensestracker.android.ui.theme.DodgerBlue

@Composable
fun RecurrentIncomesScreen(viewModel: RecurrentIncomesViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    EntryListScreen(
        state = uiState.list,
        onRefresh = viewModel::refresh,
        onAdd = viewModel::openNew,
        addDescription = "Nuevo ingreso recurrente",
        emptyText = "No hay ingresos recurrentes qué mostrar",
        endText = "No hay más ingresos recurrentes qué mostrar",
        key = RecurrentIncomeEntity::uuid,
    ) { item ->
        EntryCard(item.description, item.valIncome, onClick = { viewModel.openEdit(item) }) {
            Text(formatDate(item.createdAt), style = MaterialTheme.typography.labelSmall, color = DodgerBlue.c500)
            EnabledStatus(item.enabled)
        }
    }

    uiState.editor?.let { editor ->
        val item = editor.item
        val form = remember(editor) { EntryForm(item?.valIncome, item?.description.orEmpty()) }
        var enabled by remember(editor) { mutableStateOf(item?.enabled ?: true) }
        EntryFormDialog(
            form = form,
            title = if (item == null) "Nuevo ingreso recurrente" else "Editar ingreso recurrente",
            amountLabel = "Total del ingreso",
            descriptionLabel = "Descripción del ingreso",
            isEditing = item != null,
            isSaving = uiState.isSaving,
            deleteQuestion = "¿Eliminar este ingreso recurrente?",
            onDismiss = viewModel::closeEditor,
            onSave = { values, createAnother -> viewModel.save(values, enabled, createAnother) },
            onDelete = viewModel::delete,
        ) {
            if (item != null) LabeledSwitch("¿Habilitado?", enabled, { enabled = it }, enabled = !uiState.isSaving)
        }
    }
}
