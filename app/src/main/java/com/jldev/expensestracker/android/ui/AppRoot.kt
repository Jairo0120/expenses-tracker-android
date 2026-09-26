package com.jldev.expensestracker.android.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jldev.expensestracker.android.auth.AuthRepository
import com.jldev.expensestracker.android.ui.login.LoginScreen
import com.jldev.expensestracker.android.ui.shell.AppShell
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(authRepository: AuthRepository) : ViewModel() {
    val isSignedIn = authRepository.isSignedIn
}

/** Shows login until there is a session; falls back to it on logout or when the session expires. */
@Composable
fun AppRoot(viewModel: AppViewModel = hiltViewModel()) {
    val isSignedIn by viewModel.isSignedIn.collectAsStateWithLifecycle()
    if (isSignedIn) AppShell() else LoginScreen()
}
