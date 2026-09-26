package com.jldev.expensestracker.android.ui.shell

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auth0.android.authentication.AuthenticationException
import com.jldev.expensestracker.android.auth.AuthRepository
import com.jldev.expensestracker.android.data.CycleRepository
import com.jldev.expensestracker.android.data.remote.model.CycleListItem
import com.jldev.expensestracker.android.data.remote.model.CycleStatus
import com.jldev.expensestracker.android.data.remote.toUserMessage
import com.jldev.expensestracker.android.ui.UserMessages
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShellUiState(
    val cycles: List<CycleListItem> = emptyList(),
    val selectedCycleId: Int? = null,
    val status: CycleStatus? = null,
)

@HiltViewModel
class ShellViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val cycleRepository: CycleRepository,
    private val userMessages: UserMessages,
) : ViewModel() {

    val uiState: StateFlow<ShellUiState> = combine(
        cycleRepository.cycles,
        cycleRepository.selectedCycleId,
        cycleRepository.status,
        ::ShellUiState,
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShellUiState())

    val messages = userMessages.messages

    init {
        // This ViewModel lives as long as the Activity, across logout/login, so work is keyed on the session.
        viewModelScope.launch {
            authRepository.isSignedIn.collectLatest { signedIn ->
                if (!signedIn) {
                    cycleRepository.clear()
                    return@collectLatest
                }
                launch { loadCycles() }
                cycleRepository.selectedCycleId.collectLatest { refreshStatus() }
            }
        }
    }

    fun selectCycle(cycleId: Int?) = cycleRepository.select(cycleId)

    fun refresh() {
        viewModelScope.launch { loadCycles() }
        viewModelScope.launch { refreshStatus() }
    }

    fun logout(activityContext: Context) {
        viewModelScope.launch {
            try {
                authRepository.logout(activityContext)
            } catch (e: AuthenticationException) {
                if (!e.isCanceled) userMessages.show(e.getDescription())
            }
        }
    }

    private suspend fun loadCycles() = reportingErrors("Error al obtener la lista de ciclos") {
        cycleRepository.loadCycles()
    }

    private suspend fun refreshStatus() = reportingErrors("Error al obtener el estado del ciclo") {
        cycleRepository.refreshStatus()
    }

    private suspend fun reportingErrors(prefix: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // A lost session is handled by AppRoot switching to login.
            if (authRepository.isSignedIn.value) userMessages.show("$prefix: ${e.toUserMessage()}")
        }
    }
}
