package com.jldev.expensestracker.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jldev.expensestracker.android.ui.common.ListState

/** Pull-to-refresh list of the items in the local database. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> EntryLazyColumn(
    state: ListState<T>,
    onRefresh: () -> Unit,
    key: (T) -> Any,
    emptyText: String,
    /** Shown under the last item; null for none. */
    endText: String?,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    itemContent: @Composable (T) -> Unit,
) {
    // Another cycle or filter starts from the top.
    val firstKey = state.items.firstOrNull()?.let(key)
    LaunchedEffect(firstKey) {
        if (firstKey != null) listState.scrollToItem(0)
    }

    PullToRefreshBox(isRefreshing = state.isRefreshing, onRefresh = onRefresh, modifier = modifier) {
        LazyColumn(
            state = listState,
            // Bottom padding keeps the last card clear of the add button.
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            // Keyed items only: an always-present footer would become the scroll anchor while the list is
            // empty, and the first items would push the view to the end.
            if (state.loaded && !state.isRefreshing && state.items.isEmpty()) {
                item(key = "empty") { FooterText(emptyText) }
            }
            items(state.items, key = key) { itemContent(it) }
            if (state.items.isNotEmpty() && endText != null) {
                item(key = "footer") { FooterText(endText) }
            }
        }
    }
}

@Composable
private fun FooterText(text: String) {
    Text(
        text,
        color = Color.White,
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
    )
}
