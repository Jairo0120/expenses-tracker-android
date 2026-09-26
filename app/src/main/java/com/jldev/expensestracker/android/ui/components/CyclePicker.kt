package com.jldev.expensestracker.android.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jldev.expensestracker.android.R
import com.jldev.expensestracker.android.data.local.CycleEntity
import com.jldev.expensestracker.android.ui.format.formatMonth
import com.jldev.expensestracker.android.ui.format.formatShortDate
import com.jldev.expensestracker.android.ui.theme.DodgerBlue

/** How many of the newest cycles the menu lists before "Ver más". */
const val RECENT_CYCLES = 24

/** The cycles the menu lists: the newest [limit] unless [showAll]. */
fun visibleCycles(cycles: List<CycleEntity>, showAll: Boolean, limit: Int = RECENT_CYCLES): List<CycleEntity> =
    if (showAll) cycles else cycles.take(limit)

/**
 * Compact cycle selector (a chip with a menu grouped by year) above the cycle-scoped lists. The menu
 * lists the last two years; "Ver más" shows the rest.
 */
@Composable
fun CyclePicker(
    cycles: List<CycleEntity>,
    /** The cycle being shown. */
    selectedCycleUuid: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var showAll by remember { mutableStateOf(false) }
    val shown = cycles.firstOrNull { it.uuid == selectedCycleUuid }

    Box(modifier) {
        AssistChip(
            onClick = {
                // Open expanded when the selected cycle is older than the recent ones, so it's listed.
                showAll = cycles.indexOf(shown) >= RECENT_CYCLES
                expanded = true
            },
            enabled = cycles.isNotEmpty(),
            label = { Text(shown?.let { formatShortDate(it.startDate) } ?: "Ciclo") },
            trailingIcon = {
                Icon(painterResource(R.drawable.ic_arrow_drop_down), contentDescription = null, Modifier.size(18.dp))
            },
            colors = AssistChipDefaults.assistChipColors(
                labelColor = Color.White,
                trailingIconContentColor = DodgerBlue.c300,
            ),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            val listed = visibleCycles(cycles, showAll)
            // The list comes newest first, so years come out in descending order.
            listed.groupBy { it.startDate.year }.forEach { (year, yearCycles) ->
                if (year != cycles.first().startDate.year) HorizontalDivider()
                Text(
                    year.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = DodgerBlue.c300,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
                yearCycles.forEach { cycle ->
                    DropdownMenuItem(
                        text = { Text(formatMonth(cycle.startDate)) },
                        onClick = {
                            onSelect(cycle.uuid)
                            expanded = false
                        },
                        trailingIcon = if (cycle.uuid == shown?.uuid) {
                            { Icon(painterResource(R.drawable.ic_check), contentDescription = "Seleccionado") }
                        } else {
                            null
                        },
                    )
                }
            }
            if (listed.size < cycles.size) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Ver más", color = DodgerBlue.c300, fontWeight = FontWeight.SemiBold) },
                    onClick = { showAll = true },
                )
            }
        }
    }
}
