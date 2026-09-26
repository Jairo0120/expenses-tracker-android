package com.jldev.expensestracker.android.ui.shell

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auth0.android.authentication.AuthenticationException
import com.jldev.expensestracker.android.auth.AuthRepository
import com.jldev.expensestracker.android.data.CycleRepository
import com.jldev.expensestracker.android.data.model.CycleStatus
import com.jldev.expensestracker.android.data.sync.SyncRepository
import com.jldev.expensestracker.android.ui.UserMessages
import com.jldev.expensestracker.android.ui.common.runReporting
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShellUiState(
    val status: CycleStatus? = null,
)

@HiltViewModel
class ShellViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    cycleRepository: CycleRepository,
    private val syncRepository: SyncRepository,
    private val userMessages: UserMessages,
) : ViewModel() {

    val uiState: StateFlow<ShellUiState> = cycleRepository.status
        .map(::ShellUiState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShellUiState())

    val messages = userMessages.messages

    init {
        // Screens show the local copy right away; this brings in what changed on the server.
        viewModelScope.launch {
            userMessages.runReporting("No se pudo sincronizar") { syncRepository.sync() }
        }
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
}
