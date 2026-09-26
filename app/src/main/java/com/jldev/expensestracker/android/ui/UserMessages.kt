package com.jldev.expensestracker.android.ui

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Transient messages shown in the shell's snackbar (the Expo app's FlashMessage). */
@Singleton
class UserMessages @Inject constructor() {
    private val channel = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = channel.receiveAsFlow()

    fun show(message: String) {
        channel.trySend(message)
    }
}
