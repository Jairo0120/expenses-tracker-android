package com.jldev.expensestracker.android.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jldev.expensestracker.android.ui.common.ListState

/**
 * A tab's content: optional [header] (filters) and the list. Its add button lives in the shell and
 * hides while the list scrolls down, so it doesn't cover the cards being read.
 */
@Composable
fun <T> EntryListScreen(
    state: ListState<T>,
    onRefresh: () -> Unit,
    onAdd: () -> Unit,
    addDescription: String,
    emptyText: String,
    endText: String?,
    key: (T) -> Any,
    header: @Composable ColumnScope.() -> Unit = {},
    itemContent: @Composable (T) -> Unit,
) {
    val listState = rememberLazyListState()
    PublishAddButton(AddAction(addDescription, visible = !listState.lastScrolledForward, onClick = onAdd))

    Column(Modifier.fillMaxSize()) {
        header()
        EntryLazyColumn(
            state = state,
            onRefresh = onRefresh,
            key = key,
            emptyText = emptyText,
            endText = endText,
            listState = listState,
            modifier = Modifier.weight(1f),
            itemContent = itemContent,
        )
    }
}
