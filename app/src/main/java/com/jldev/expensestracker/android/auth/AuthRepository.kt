package com.jldev.expensestracker.android.auth

import android.content.Context
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationAPIClient
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.authentication.storage.CredentialsManager
import com.auth0.android.authentication.storage.CredentialsManagerException
import com.auth0.android.authentication.storage.SharedPreferencesStorage
import com.auth0.android.provider.WebAuthProvider
import com.auth0.android.result.Credentials
import com.jldev.expensestracker.android.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(@ApplicationContext context: Context) {

    private val account = Auth0.getInstance(BuildConfig.AUTH0_CLIENT_ID, BuildConfig.AUTH0_DOMAIN)

    private val credentialsManager = CredentialsManager(
        AuthenticationAPIClient(account),
        SharedPreferencesStorage(context),
    )

    private val _isSignedIn = MutableStateFlow(credentialsManager.hasValidCredentials())

    /** Turns false on logout or when the stored session can no longer be renewed. */
    val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    /** Opens Universal Login. [activityContext] must be an Activity so the browser can be launched. */
    suspend fun login(activityContext: Context) {
        val credentials = WebAuthProvider.login(account)
            .withScheme(BuildConfig.AUTH0_SCHEME)
            .withAudience(BuildConfig.AUTH0_AUDIENCE)
            .withScope("openid profile email offline_access")
            .await(activityContext)
        credentialsManager.saveCredentials(credentials)
        _isSignedIn.value = true
    }

    /**
     * Returns stored credentials, refreshing them with the refresh token when about to expire.
     * Throws [IOException] when the refresh failed for lack of network (the session is kept), and
     * [CredentialsManagerException] when the session is gone, after marking the user signed out.
     */
    suspend fun credentials(): Credentials = try {
        credentialsManager.awaitCredentials()
    } catch (e: CredentialsManagerException) {
        if ((e.cause as? AuthenticationException)?.isNetworkError == true) {
            throw IOException("Could not renew the session: no network", e)
        }
        _isSignedIn.value = false
        throw e
    }

    suspend fun accessToken(): String = credentials().accessToken

    suspend fun logout(activityContext: Context) {
        WebAuthProvider.logout(account)
            .withScheme(BuildConfig.AUTH0_SCHEME)
            .await(activityContext)
        credentialsManager.clearCredentials()
        _isSignedIn.value = false
    }
}
