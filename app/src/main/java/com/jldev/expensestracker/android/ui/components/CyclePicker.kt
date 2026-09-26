package com.jldev.expensestracker.android.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.jldev.expensestracker.android.data.remote.model.CycleListItem
import com.jldev.expensestracker.android.ui.format.formatShortDate
import java.time.LocalDate

/** Cycle selector shown on top of the cycle-scoped lists. A null [selectedCycleId] is the active cycle. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CyclePicker(
    cycles: List<CycleListItem>,
    selectedCycleId: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = cycles.firstOrNull { it.id == selectedCycleId }
    // Like the Expo dropdown's placeholder: the active cycle is labelled with the current month.
    val label = formatShortDate(selected?.startDate ?: LocalDate.now())

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it && cycles.isNotEmpty() },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text("Ciclo") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            cycles.forEach { cycle ->
                DropdownMenuItem(
                    text = { Text(formatShortDate(cycle.startDate)) },
                    onClick = {
                        onSelect(cycle.id)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}
