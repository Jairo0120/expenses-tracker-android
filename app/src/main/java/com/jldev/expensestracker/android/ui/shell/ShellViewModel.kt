package com.jldev.expensestracker.android.ui.shell

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auth0.android.authentication.AuthenticationException
import com.jldev.expensestracker.android.auth.AuthRepository
import com.jldev.expensestracker.android.data.CycleRepository
import com.jldev.expensestracker.android.data.model.CycleStatus
import com.jldev.expensestracker.android.data.sync.SyncRepository
import com.jldev.expensestracker.android.data.sync.SyncScheduler
import com.jldev.expensestracker.android.ui.UserMessages
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShellUiState(
    val status: CycleStatus? = null,
    /** Local changes the server hasn't received yet. */
    val pendingChanges: Int = 0,
    /** Set while asking whether to log out with changes still unsent (their number). */
    val unsentChangesOnLogout: Int? = null,
)

@HiltViewModel
class ShellViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    cycleRepository: CycleRepository,
    private val syncRepository: SyncRepository,
    private val syncScheduler: SyncScheduler,
    private val userMessages: UserMessages,
) : ViewModel() {

    private val unsentChangesOnLogout = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<ShellUiState> = combine(
        cycleRepository.status,
        syncRepository.pendingCount,
        unsentChangesOnLogout,
        ::ShellUiState,
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShellUiState())

    val messages = userMessages.messages

    init {
        // Screens show the local copy right away; syncs bring in what changed, whenever there's a connection.
        syncScheduler.startPeriodic()
        syncScheduler.syncSoon()
        viewModelScope.launch {
            syncRepository.rejections.collect { rejection ->
                userMessages.show("El servidor rechazó un cambio: ${rejection.reason ?: "sin detalle"}")
            }
        }
    }

    /** Logs out, first asking when there are changes the server hasn't received (logging out loses them). */
    fun logout(activityContext: Context) {
        viewModelScope.launch {
            val pending = syncRepository.pendingCount.first()
            if (pending > 0 && unsentChangesOnLogout.value == null) {
                unsentChangesOnLogout.value = pending
                return@launch
            }
            unsentChangesOnLogout.value = null
            try {
                authRepository.logout(activityContext)
            } catch (e: AuthenticationException) {
                if (!e.isCanceled) userMessages.show(e.getDescription())
            }
        }
    }

    fun cancelLogout() {
        unsentChangesOnLogout.value = null
    }
}
