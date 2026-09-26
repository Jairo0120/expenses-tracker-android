package com.jldev.expensestracker.android.ui.common

import com.jldev.expensestracker.android.data.remote.api.DEFAULT_PAGE_SIZE
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PagedState<T>(
    val items: List<T> = emptyList(),
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    /** The last page came back short, so there is nothing more to load. */
    val endReached: Boolean = false,
    /** A first page loaded since the last refresh; false after a failed refresh. */
    val loaded: Boolean = false,
)

/** skip/limit pagination over the backend's list endpoints, like the Expo app's infinite lists. */
class PagedList<T>(
    private val scope: CoroutineScope,
    private val pageSize: Int = DEFAULT_PAGE_SIZE,
    private val onError: (Exception) -> Unit,
    private val fetchPage: suspend (skip: Int, limit: Int) -> List<T>,
) {
    private val _state = MutableStateFlow(PagedState<T>())
    val state: StateFlow<PagedState<T>> = _state.asStateFlow()

    private var job: Job? = null

    /** Reloads from the first page, discarding any load in flight (which may be for a previous filter). */
    fun refresh() {
        job?.cancel()
        _state.update { it.copy(isRefreshing = true, isLoadingMore = false) }
        job = scope.launch { load(skip = 0) }
    }

    fun loadMore() {
        val current = _state.value
        if (job?.isActive == true || !current.loaded || current.endReached) return
        _state.update { it.copy(isLoadingMore = true) }
        job = scope.launch { load(skip = current.items.size) }
    }

    private suspend fun load(skip: Int) {
        val isFirstPage = skip == 0
        try {
            val page = fetchPage(skip, pageSize)
            _state.update {
                PagedState(
                    items = if (isFirstPage) page else it.items + page,
                    endReached = page.size < pageSize,
                    loaded = true,
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.update {
                // A failed refresh drops the old items: they may belong to another cycle or filter.
                if (isFirstPage) PagedState() else it.copy(isLoadingMore = false)
            }
            onError(e)
        }
    }
}
