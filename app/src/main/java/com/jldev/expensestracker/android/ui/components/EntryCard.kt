package com.jldev.expensestracker.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jldev.expensestracker.android.ui.format.formatMoney
import com.jldev.expensestracker.android.ui.theme.DodgerBlue

/** The list card used throughout the Expo app: description and amount on the left, details on the right. */
@Composable
fun EntryCard(
    description: String,
    amount: Double,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    middle: @Composable RowScope.() -> Unit = {},
    trailing: @Composable ColumnScope.() -> Unit = {},
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = DodgerBlue.c950),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = DodgerBlue.c300,
                )
                Text(formatMoney(amount), style = MaterialTheme.typography.titleLarge, color = DodgerBlue.c100)
            }
            middle()
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp),
                content = trailing,
            )
        }
    }
}
