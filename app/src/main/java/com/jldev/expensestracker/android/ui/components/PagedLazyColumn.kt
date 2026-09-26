package com.jldev.expensestracker.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jldev.expensestracker.android.ui.common.PagedState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/** Pull-to-refresh list that asks for the next page when the user nears the end. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> PagedLazyColumn(
    state: PagedState<T>,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    key: (T) -> Any,
    emptyText: String,
    /** Shown once the last page is in; null for lists that aren't paged. */
    endText: String?,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    itemContent: @Composable (T) -> Unit,
) {
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)

    // A refresh for another cycle or filter starts from the top.
    val firstKey = state.items.firstOrNull()?.let(key)
    LaunchedEffect(firstKey) {
        if (firstKey != null) listState.scrollToItem(0)
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            // Include the count so a newly loaded page that still fits on screen asks for the next one.
            (info.totalItemsCount > 0 && lastVisible >= info.totalItemsCount - 3) to info.totalItemsCount
        }
            .distinctUntilChanged()
            .filter { (nearEnd, _) -> nearEnd }
            .collect { currentOnLoadMore() }
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
            // empty, so the first page would push the view to the end and trigger "load more".
            if (state.loaded && !state.isRefreshing && state.items.isEmpty()) {
                item(key = "empty") { FooterText(emptyText) }
            }
            items(state.items, key = key) { itemContent(it) }
            if (state.items.isNotEmpty()) {
                item(key = "footer") {
                    if (state.isLoadingMore) {
                        Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    } else if (state.endReached && endText != null) {
                        FooterText(endText)
                    }
                }
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
