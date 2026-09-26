package com.jldev.expensestracker.android.ui.common

import com.jldev.expensestracker.android.data.remote.NotAuthenticatedException
import com.jldev.expensestracker.android.data.remote.toUserMessage
import com.jldev.expensestracker.android.ui.UserMessages
import kotlinx.coroutines.CancellationException

/** Shows "[failureMessage]: reason", except for a lost session, which AppRoot handles by showing login. */
fun UserMessages.report(failureMessage: String, error: Exception) {
    if (error !is NotAuthenticatedException) show("$failureMessage: ${error.toUserMessage()}")
}

/** Runs [block], reporting any failure. Returns whether it succeeded. */
suspend fun UserMessages.runReporting(failureMessage: String, block: suspend () -> Unit): Boolean =
    try {
        block()
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        report(failureMessage, e)
        false
    }
