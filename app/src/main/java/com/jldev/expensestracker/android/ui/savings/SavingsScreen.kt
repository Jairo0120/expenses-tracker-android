package com.jldev.expensestracker.android.ui.savings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jldev.expensestracker.android.data.model.SavingItem
import com.jldev.expensestracker.android.ui.components.CycleHeader
import com.jldev.expensestracker.android.ui.components.EntryCard
import com.jldev.expensestracker.android.ui.components.EntryListScreen
import com.jldev.expensestracker.android.ui.components.RecurrentMarker
import com.jldev.expensestracker.android.ui.format.formatDate
import com.jldev.expensestracker.android.ui.theme.DodgerBlue

@Composable
fun SavingsScreen(viewModel: SavingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    EntryListScreen(
        state = uiState.list,
        onRefresh = viewModel::refresh,
        onAdd = viewModel::openNew,
        addDescription = "Nuevo ahorro",
        emptyText = "No hay ahorros qué mostrar",
        endText = "No hay más ahorros qué mostrar",
        key = { it.saving.uuid },
        header = { CycleHeader(viewModel) },
    ) { item ->
        val saving = item.saving
        EntryCard(
            description = item.savingTypeDescription,
            amount = saving.valSaving,
            onClick = { viewModel.openEdit(item) },
            middle = { if (saving.isRecurrentSaving) RecurrentMarker("Ahorro recurrente") },
        ) {
            if (saving.isRecurrentSaving) {
                Text(
                    "Ahorro recurrente",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = DodgerBlue.c300,
                )
            }
            Text(formatDate(saving.dateSaving), style = MaterialTheme.typography.labelSmall, color = DodgerBlue.c500)
        }
    }

    uiState.editor?.let { editor ->
        SavingFormDialog(
            editorKey = editor,
            saving = editor.item,
            isSaving = uiState.isSaving,
            onDismiss = viewModel::closeEditor,
            onSave = viewModel::save,
            onDelete = viewModel::delete,
        )
    }
}
