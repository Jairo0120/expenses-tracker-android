package com.jldev.expensestracker.android.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.jldev.expensestracker.android.auth.AuthRepository
import com.jldev.expensestracker.android.data.CycleRepository
import com.jldev.expensestracker.android.data.sync.SyncRepository
import com.jldev.expensestracker.android.data.sync.SyncScheduler
import com.jldev.expensestracker.android.ui.login.LoginScreen
import com.jldev.expensestracker.android.ui.shell.AppShell
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    authRepository: AuthRepository,
    cycleRepository: CycleRepository,
    syncRepository: SyncRepository,
    syncScheduler: SyncScheduler,
) : ViewModel() {
    val isSignedIn = authRepository.isSignedIn

    /**
     * Holds the signed-in UI's ViewModels: they survive configuration changes like any other, but are
     * cleared on logout so the next session starts fresh.
     */
    var sessionViewModelStore = ViewModelStore()
        private set

    init {
        viewModelScope.launch {
            isSignedIn.collect { signedIn ->
                if (!signedIn) {
                    sessionViewModelStore.clear()
                    sessionViewModelStore = ViewModelStore()
                    // The next user must not see this one's data.
                    syncScheduler.stop()
                    cycleRepository.reset()
                    syncRepository.clear()
                }
            }
        }
    }

    override fun onCleared() = sessionViewModelStore.clear()
}

/** Scopes ViewModels to [store] while creating them with the Activity's (Hilt) factory. */
private class SessionViewModelStoreOwner(
    override val viewModelStore: ViewModelStore,
    private val parent: HasDefaultViewModelProviderFactory,
) : ViewModelStoreOwner, HasDefaultViewModelProviderFactory by parent

/** Shows login until there is a session; falls back to it on logout or when the session expires. */
@Composable
fun AppRoot(viewModel: AppViewModel = hiltViewModel()) {
    val isSignedIn by viewModel.isSignedIn.collectAsStateWithLifecycle()
    if (!isSignedIn) {
        LoginScreen()
        return
    }
    val activityOwner = checkNotNull(LocalViewModelStoreOwner.current)
    val store = viewModel.sessionViewModelStore
    val sessionOwner = remember(store, activityOwner) {
        SessionViewModelStoreOwner(store, activityOwner as HasDefaultViewModelProviderFactory)
    }
    CompositionLocalProvider(LocalViewModelStoreOwner provides sessionOwner) {
        AppShell()
    }
}
