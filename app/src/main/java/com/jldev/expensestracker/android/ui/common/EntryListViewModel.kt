package com.jldev.expensestracker.android.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jldev.expensestracker.android.ui.UserMessages
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The form dialog being shown: [item] is null when creating. [key] changes on "Crear otro" so the
 * dialog starts a fresh, empty form.
 */
data class EditorState<T>(val item: T? = null, val key: Int = 0)

data class EntryListUiState<T>(
    val list: PagedState<T> = PagedState(),
    val editor: EditorState<T>? = null,
    val isSaving: Boolean = false,
)

/**
 * A paged list whose items are created, edited and deleted through a form dialog; the shape shared by
 * every list in the Expo app. Subclasses call [refresh] from their init block.
 */
abstract class EntryListViewModel<T : Any>(
    protected val userMessages: UserMessages,
    loadErrorMessage: String,
) : ViewModel() {

    protected val pagedList = PagedList(
        scope = viewModelScope,
        onError = { userMessages.report(loadErrorMessage, it) },
        fetchPage = { skip, limit -> fetchPage(skip, limit) },
    )

    private val editor = MutableStateFlow<EditorState<T>?>(null)
    private val isSaving = MutableStateFlow(false)

    val uiState: StateFlow<EntryListUiState<T>> =
        combine(pagedList.state, editor, isSaving, ::EntryListUiState)
            .stateIn(viewModelScope, SharingStarted.Eagerly, EntryListUiState())

    protected abstract suspend fun fetchPage(skip: Int, limit: Int): List<T>

    /** Called after a successful create, update or delete. */
    protected open fun onChanged() = refresh()

    fun refresh() = pagedList.refresh()

    fun loadMore() = pagedList.loadMore()

    fun openNew() {
        editor.value = EditorState()
    }

    fun openEdit(item: T) {
        editor.value = EditorState(item)
    }

    fun closeEditor() {
        if (!isSaving.value) editor.value = null
    }

    /** Runs [block] with the item being edited (null when creating), then closes or resets the dialog. */
    protected fun submit(
        successMessage: String,
        failureMessage: String,
        createAnother: Boolean = false,
        block: suspend (item: T?) -> Unit,
    ) {
        val current = editor.value ?: return
        if (isSaving.value) return
        isSaving.value = true
        viewModelScope.launch {
            val succeeded = userMessages.runReporting(failureMessage) { block(current.item) }
            isSaving.value = false
            if (!succeeded) return@launch
            userMessages.show(successMessage)
            editor.update { if (createAnother && it != null) EditorState(key = it.key + 1) else null }
            onChanged()
        }
    }
}
