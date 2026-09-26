package com.jldev.expensestracker.android.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jldev.expensestracker.android.ui.common.CycleEntryListViewModel

/** The cycle picker above a cycle-scoped list. */
@Composable
fun CycleHeader(viewModel: CycleEntryListViewModel<*>) {
    val cycles by viewModel.cycles.collectAsStateWithLifecycle()
    val selectedCycleId by viewModel.selectedCycleId.collectAsStateWithLifecycle()
    CyclePicker(
        cycles = cycles,
        selectedCycleId = selectedCycleId,
        onSelect = viewModel::selectCycle,
        modifier = Modifier.padding(horizontal = 12.dp),
    )
}
