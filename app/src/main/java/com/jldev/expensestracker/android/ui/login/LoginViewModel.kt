package com.jldev.expensestracker.android.ui.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auth0.android.authentication.AuthenticationException
import com.jldev.expensestracker.android.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LoginUiState {
    data class SignedOut(val error: String? = null) : LoginUiState
    data object SigningIn : LoginUiState
    data object SignedIn : LoginUiState
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(
        if (authRepository.hasValidCredentials()) LoginUiState.SignedIn else LoginUiState.SignedOut()
    )
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(activityContext: Context) {
        if (_uiState.value == LoginUiState.SigningIn) return
        _uiState.value = LoginUiState.SigningIn
        viewModelScope.launch {
            _uiState.value = try {
                authRepository.login(activityContext)
                LoginUiState.SignedIn
            } catch (e: AuthenticationException) {
                LoginUiState.SignedOut(error = if (e.isCanceled) null else e.getDescription())
            }
        }
    }
}
