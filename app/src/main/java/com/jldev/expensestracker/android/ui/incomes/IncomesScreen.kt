package com.jldev.expensestracker.android.ui.incomes

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jldev.expensestracker.android.data.remote.model.Income
import com.jldev.expensestracker.android.ui.components.CycleHeader
import com.jldev.expensestracker.android.ui.components.EntryCard
import com.jldev.expensestracker.android.ui.components.EntryForm
import com.jldev.expensestracker.android.ui.components.EntryFormDialog
import com.jldev.expensestracker.android.ui.components.EntryListScreen
import com.jldev.expensestracker.android.ui.components.LabeledSwitch
import com.jldev.expensestracker.android.ui.components.RecurrentMarker
import com.jldev.expensestracker.android.ui.format.formatDate
import com.jldev.expensestracker.android.ui.theme.DodgerBlue

@Composable
fun IncomesScreen(viewModel: IncomesViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    EntryListScreen(
        state = uiState.list,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onAdd = viewModel::openNew,
        addDescription = "Nuevo ingreso",
        emptyText = "No hay ingresos qué mostrar",
        endText = "No hay más ingresos qué mostrar",
        key = Income::id,
        header = { CycleHeader(viewModel) },
    ) { income ->
        EntryCard(
            description = income.description,
            amount = income.valIncome,
            onClick = { viewModel.openEdit(income) },
            middle = { if (income.isRecurrentIncome) RecurrentMarker("Ingreso recurrente") },
        ) {
            if (income.isRecurrentIncome) {
                Text(
                    "Ingreso recurrente",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = DodgerBlue.c300,
                )
            }
            Text(formatDate(income.dateIncome), style = MaterialTheme.typography.labelSmall, color = DodgerBlue.c500)
        }
    }

    uiState.editor?.let { editor ->
        val income = editor.item
        val form = remember(editor) { EntryForm(income?.valIncome, income?.description.orEmpty()) }
        var createRecurrent by remember(editor) { mutableStateOf(false) }
        EntryFormDialog(
            form = form,
            title = if (income == null) "Nuevo ingreso" else "Editar ingreso",
            amountLabel = "Total del ingreso",
            descriptionLabel = "Descripción del ingreso",
            isEditing = income != null,
            isSaving = uiState.isSaving,
            deleteQuestion = "¿Eliminar este ingreso?",
            onDismiss = viewModel::closeEditor,
            onSave = { values, createAnother -> viewModel.save(values, createRecurrent, createAnother) },
            onDelete = viewModel::delete,
        ) {
            if (income == null) {
                LabeledSwitch("¿Ingreso mensual?", createRecurrent, { createRecurrent = it }, enabled = !uiState.isSaving)
            }
        }
    }
}
