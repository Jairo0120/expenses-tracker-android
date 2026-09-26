package com.jldev.expensestracker.android.ui.shell

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auth0.android.authentication.AuthenticationException
import com.jldev.expensestracker.android.auth.AuthRepository
import com.jldev.expensestracker.android.data.CycleRepository
import com.jldev.expensestracker.android.data.remote.model.CycleListItem
import com.jldev.expensestracker.android.data.remote.model.CycleStatus
import com.jldev.expensestracker.android.ui.UserMessages
import com.jldev.expensestracker.android.ui.common.runReporting
import dagger.hilt.android.lifecycle.HiltViewModel
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
        viewModelScope.launch { loadCycles() }
        // Emits the current cycle first, which does the initial load.
        viewModelScope.launch { cycleRepository.selectedCycleId.collectLatest { refreshStatus() } }
    }

    fun selectCycle(cycleId: Int) = cycleRepository.select(cycleId)

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

    private suspend fun loadCycles() {
        userMessages.runReporting("Error al obtener la lista de ciclos") { cycleRepository.loadCycles() }
    }

    private suspend fun refreshStatus() {
        userMessages.runReporting("Error al obtener el estado del ciclo") { cycleRepository.refreshStatus() }
    }
}
