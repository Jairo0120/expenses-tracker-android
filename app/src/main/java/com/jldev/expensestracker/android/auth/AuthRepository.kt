package com.jldev.expensestracker.android.auth

import android.content.Context
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationAPIClient
import com.auth0.android.authentication.storage.CredentialsManager
import com.auth0.android.authentication.storage.SharedPreferencesStorage
import com.auth0.android.provider.WebAuthProvider
import com.auth0.android.result.Credentials
import com.jldev.expensestracker.android.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(@ApplicationContext context: Context) {

    private val account = Auth0.getInstance(BuildConfig.AUTH0_CLIENT_ID, BuildConfig.AUTH0_DOMAIN)

    private val credentialsManager = CredentialsManager(
        AuthenticationAPIClient(account),
        SharedPreferencesStorage(context),
    )

    fun hasValidCredentials(): Boolean = credentialsManager.hasValidCredentials()

    /** Opens Universal Login. [activityContext] must be an Activity so the browser can be launched. */
    suspend fun login(activityContext: Context) {
        val credentials = WebAuthProvider.login(account)
            .withScheme(BuildConfig.AUTH0_SCHEME)
            .withAudience(BuildConfig.AUTH0_AUDIENCE)
            .withScope("openid profile email offline_access")
            .await(activityContext)
        credentialsManager.saveCredentials(credentials)
    }

    /** Returns stored credentials, refreshing them with the refresh token when about to expire. */
    suspend fun credentials(): Credentials = credentialsManager.awaitCredentials()

    suspend fun accessToken(): String = credentials().accessToken

    suspend fun logout(activityContext: Context) {
        WebAuthProvider.logout(account)
            .withScheme(BuildConfig.AUTH0_SCHEME)
            .await(activityContext)
        credentialsManager.clearCredentials()
    }
}
