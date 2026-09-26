package com.jldev.expensestracker.android.ui.savings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jldev.expensestracker.android.data.remote.model.GroupedSaving
import com.jldev.expensestracker.android.ui.components.EntryListScreen
import com.jldev.expensestracker.android.ui.components.RecurrentMarker
import com.jldev.expensestracker.android.ui.format.formatMoney
import com.jldev.expensestracker.android.ui.theme.DodgerBlue

@Composable
fun SavingsSummaryScreen(viewModel: SavingsSummaryViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refresh() }

    EntryListScreen(
        state = uiState.list,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onAdd = viewModel::openNew,
        addDescription = "Nuevo ahorro",
        emptyText = "No hay ahorros qué mostrar",
        endText = "",
        key = GroupedSaving::id,
    ) { SavingTypeCard(it) }

    uiState.editor?.let { editor ->
        SavingFormDialog(
            editorKey = editor,
            saving = null,
            isSaving = uiState.isSaving,
            onDismiss = viewModel::closeEditor,
            onSave = viewModel::save,
        )
    }
}

@Composable
private fun SavingTypeCard(saving: GroupedSaving) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DodgerBlue.c950),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (saving.isRecurrentSaving) RecurrentMarker("Ahorro recurrente")
                Text(
                    saving.description,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = DodgerBlue.c300,
                )
            }
            Row(Modifier.padding(top = 4.dp)) {
                Total("Este mes", saving.totalLastMonth, Alignment.End, Modifier.weight(1f).padding(end = 8.dp))
                Total("Totalizado", saving.totalGlobal, Alignment.Start, Modifier.weight(1f).padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun Total(label: String, amount: Double, alignment: Alignment.Horizontal, modifier: Modifier) {
    Column(modifier, horizontalAlignment = alignment) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = DodgerBlue.c400)
        Text(formatMoney(amount), style = MaterialTheme.typography.titleLarge, color = DodgerBlue.c100)
    }
}
