package com.jldev.expensestracker.android.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.authentication.storage.CredentialsManagerException
import com.jldev.expensestracker.android.auth.AuthRepository
import com.jldev.expensestracker.android.data.remote.NotAuthenticatedException
import com.jldev.expensestracker.android.data.remote.api.CyclesApi
import com.jldev.expensestracker.android.data.remote.model.CycleListItem
import com.jldev.expensestracker.android.data.remote.model.CycleStatus
import com.jldev.expensestracker.android.data.remote.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val userName: String? = null,
    val isLoading: Boolean = false,
    val status: CycleStatus? = null,
    val cycles: List<CycleListItem> = emptyList(),
    val signedOut: Boolean = false,
    val error: String? = null,
)

// Placeholder that exercises the API until the drawer/tabs shell replaces it.
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val cyclesApi: CyclesApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val user = authRepository.credentials().user
                _uiState.update { it.copy(userName = user.name ?: user.email) }
                refresh()
            } catch (e: CredentialsManagerException) {
                // Refresh token revoked or expired: back to login.
                _uiState.update { it.copy(signedOut = true) }
            }
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                // coroutineScope makes a failed request surface here instead of cancelling viewModelScope's
                // launch with an uncaught exception, which crashes the app.
                val (status, cycles) = coroutineScope {
                    val status = async { cyclesApi.getStatus() }
                    val cycles = async { cyclesApi.list() }
                    status.await() to cycles.await()
                }
                _uiState.update { it.copy(isLoading = false, status = status, cycles = cycles) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: NotAuthenticatedException) {
                _uiState.update { it.copy(isLoading = false, signedOut = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.toUserMessage()) }
            }
        }
    }

    fun logout(activityContext: Context) {
        viewModelScope.launch {
            try {
                authRepository.logout(activityContext)
                _uiState.update { it.copy(signedOut = true) }
            } catch (e: AuthenticationException) {
                if (!e.isCanceled) _uiState.update { it.copy(error = e.getDescription()) }
            }
        }
    }
}
