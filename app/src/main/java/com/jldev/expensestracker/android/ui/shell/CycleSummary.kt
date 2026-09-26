package com.jldev.expensestracker.android.ui.shell

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jldev.expensestracker.android.data.remote.model.CycleStatus
import com.jldev.expensestracker.android.ui.format.formatMoney
import com.jldev.expensestracker.android.ui.theme.DodgerBlue
import com.jldev.expensestracker.android.ui.theme.PersianRed
import com.jldev.expensestracker.android.ui.theme.Tailwind

/** How much of the cycle's budget is left, as the Expo header's color scale. */
enum class AvailableLevel { High, Medium, Low, VeryLow, Exhausted }

fun availableLevel(status: CycleStatus): AvailableLevel {
    val left = status.moneyAvailable - status.spent
    val percentLeft = if (status.moneyAvailable > 0) left / status.moneyAvailable * 100 else 0.0
    return when {
        left <= 0 -> AvailableLevel.Exhausted
        percentLeft > 70 -> AvailableLevel.High
        percentLeft > 50 -> AvailableLevel.Medium
        percentLeft > 20 -> AvailableLevel.Low
        percentLeft > 1 -> AvailableLevel.VeryLow
        else -> AvailableLevel.Exhausted
    }
}

private fun AvailableLevel.color(): Color = when (this) {
    AvailableLevel.High -> Tailwind.green500
    AvailableLevel.Medium -> Tailwind.yellow500
    AvailableLevel.Low -> Tailwind.orange500
    AvailableLevel.VeryLow -> PersianRed.c400
    AvailableLevel.Exhausted -> PersianRed.c600
}

/**
 * Header summary (the Expo app's ExpenseSummary). With income in the cycle it shows what is left to
 * spend, and a tap toggles to "spent de available"; without income it shows the total spent.
 */
@Composable
fun CycleSummary(status: CycleStatus, modifier: Modifier = Modifier) {
    var showTotals by rememberSaveable { mutableStateOf(false) }
    val style = MaterialTheme.typography.bodyMedium

    if (status.moneyAvailable <= 0) {
        Row(modifier.padding(end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Gastado: ", style = style, color = DodgerBlue.c300)
            Text(formatMoney(status.spent), style = style, color = Color.White)
        }
        return
    }

    Row(
        modifier
            .clickable { showTotals = !showTotals }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showTotals) {
            Text(
                "${formatMoney(status.spent)} de ${formatMoney(status.moneyAvailable)}",
                style = style,
                color = Color.White,
            )
        } else {
            Text("Disponible: ", style = style, color = DodgerBlue.c300)
            Text(
                formatMoney(status.moneyAvailable - status.spent),
                style = style,
                fontWeight = FontWeight.Bold,
                color = availableLevel(status).color(),
            )
        }
    }
}
