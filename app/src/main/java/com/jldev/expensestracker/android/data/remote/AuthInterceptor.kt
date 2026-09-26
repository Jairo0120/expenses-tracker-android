package com.jldev.expensestracker.android.data.remote

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

fun interface AccessTokenProvider {
    /** Returns a valid access token, refreshing it if needed; throws when there is no session. */
    suspend fun accessToken(): String
}

/** Thrown when there is no usable session; the user has to log in again. */
class NotAuthenticatedException(cause: Throwable) : IOException("Not authenticated", cause)

class AuthInterceptor(private val tokenProvider: AccessTokenProvider) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        // OkHttp only delivers IOExceptions to the caller; anything else crashes the dispatcher thread.
        val token = try {
            runBlocking { tokenProvider.accessToken() }
        } catch (e: Exception) {
            throw NotAuthenticatedException(e)
        }
        val request = chain.request().newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        return chain.proceed(request)
    }
}
