package com.jldev.expensestracker.android.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jldev.expensestracker.android.ui.common.PagedState

/** A tab's content: optional [header] (filters), the paged list, and the add button. */
@Composable
fun <T> EntryListScreen(
    state: PagedState<T>,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onAdd: () -> Unit,
    addDescription: String,
    emptyText: String,
    endText: String,
    key: (T) -> Any,
    header: @Composable ColumnScope.() -> Unit = {},
    itemContent: @Composable (T) -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            header()
            PagedLazyColumn(
                state = state,
                onRefresh = onRefresh,
                onLoadMore = onLoadMore,
                key = key,
                emptyText = emptyText,
                endText = endText,
                modifier = Modifier.weight(1f),
                itemContent = itemContent,
            )
        }
        AddButton(
            onClick = onAdd,
            contentDescription = addDescription,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        )
    }
}
