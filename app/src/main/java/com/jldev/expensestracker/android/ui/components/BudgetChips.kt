package com.jldev.expensestracker.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.jldev.expensestracker.android.data.remote.model.Budget
import com.jldev.expensestracker.android.ui.format.formatShortMoney
import com.jldev.expensestracker.android.ui.theme.DodgerBlue

/** The Expo app's BadgetPicker: a one-line chip per budget with "spent / budget"; tapping the selected one clears it. */
@Composable
fun BudgetChips(
    budgets: List<Budget>,
    selectedId: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    // Start at the selected budget, e.g. when editing an expense whose budget is far down the list.
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = budgets.indexOfFirst { it.id == selectedId }.coerceAtLeast(0),
    )
    LazyRow(state = listState, modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(budgets, key = { it.id }) { budget ->
            val selected = budget.id == selectedId
            FilterChip(
                selected = selected,
                onClick = { onSelect(if (selected) null else budget.id) },
                enabled = enabled,
                label = {
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(budget.description) }
                            append(" · ${formatShortMoney(budget.totalSpent)}")
                            if (budget.valBudget > 0) append(" / ${formatShortMoney(budget.valBudget)}")
                        },
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = DodgerBlue.c300,
                    labelColor = Color.Black,
                    selectedContainerColor = DodgerBlue.c100,
                    selectedLabelColor = Color.Black,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = enabled,
                    selected = selected,
                    borderColor = DodgerBlue.c400,
                    selectedBorderColor = DodgerBlue.c600,
                    selectedBorderWidth = 2.dp,
                ),
            )
        }
    }
}
