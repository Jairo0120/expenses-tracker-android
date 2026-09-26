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
    data class Idle(val error: String? = null) : LoginUiState
    data object SigningIn : LoginUiState
}

// AppRoot swaps this screen out once AuthRepository.isSignedIn turns true.
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(activityContext: Context) {
        if (_uiState.value == LoginUiState.SigningIn) return
        _uiState.value = LoginUiState.SigningIn
        viewModelScope.launch {
            _uiState.value = try {
                authRepository.login(activityContext)
                LoginUiState.Idle()
            } catch (e: AuthenticationException) {
                LoginUiState.Idle(error = if (e.isCanceled) null else e.getDescription())
            }
        }
    }
}
