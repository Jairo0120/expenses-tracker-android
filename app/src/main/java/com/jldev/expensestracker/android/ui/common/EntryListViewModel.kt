package com.jldev.expensestracker.android.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jldev.expensestracker.android.data.sync.SyncRepository
import com.jldev.expensestracker.android.ui.UserMessages
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The form dialog being shown: [item] is null when creating. [key] changes on "Crear otro" so the
 * dialog starts a fresh, empty form.
 */
data class EditorState<T>(val item: T? = null, val key: Int = 0)

data class ListState<T>(
    val items: List<T> = emptyList(),
    /** The local database answered at least once. */
    val loaded: Boolean = false,
    val isRefreshing: Boolean = false,
)

data class EntryListUiState<T>(
    val list: ListState<T> = ListState(),
    val editor: EditorState<T>? = null,
    val isSaving: Boolean = false,
)

/**
 * A list read from the local database whose items are created, edited and deleted through a form
 * dialog; the shape shared by every list. Saving pushes the change to the server and the list updates
 * from the database once it's stored.
 */
abstract class EntryListViewModel<T : Any>(
    protected val syncRepository: SyncRepository,
    protected val userMessages: UserMessages,
) : ViewModel() {

    /** The items to show, observed from the database. */
    protected abstract fun items(): Flow<List<T>>

    private val editor = MutableStateFlow<EditorState<T>?>(null)
    private val isSaving = MutableStateFlow(false)

    // Lazy: items() is implemented by subclasses, which aren't constructed yet while this class is.
    val uiState: StateFlow<EntryListUiState<T>> by lazy {
        combine(
            items().map { ListState(it, loaded = true) }.onStart { emit(ListState()) },
            syncRepository.isSyncing,
            editor,
            isSaving,
        ) { list, syncing, editor, saving ->
            EntryListUiState(list.copy(isRefreshing = syncing), editor, saving)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EntryListUiState())
    }

    /** Pull-to-refresh: fetches what changed on the server. */
    fun refresh() {
        viewModelScope.launch {
            userMessages.runReporting("No se pudo sincronizar") { syncRepository.sync() }
        }
    }

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
        }
    }
}
