package com.jldev.expensestracker.android.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.authentication.storage.CredentialsManagerException
import com.jldev.expensestracker.android.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val userName: String? = null,
    val signedOut: Boolean = false,
    val error: String? = null,
)

// Placeholder until the drawer/tabs shell replaces it.
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val user = authRepository.credentials().user
                _uiState.update { it.copy(userName = user.name ?: user.email) }
            } catch (e: CredentialsManagerException) {
                // Refresh token revoked or expired: back to login.
                _uiState.update { it.copy(signedOut = true) }
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
