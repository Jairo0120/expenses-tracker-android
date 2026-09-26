package com.jldev.expensestracker.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/** The current tab's add button: what it's for, whether to show it, and what it does. */
data class AddAction(val description: String, val visible: Boolean, val onClick: () -> Unit)

/**
 * Lets a tab's screen put its add button in the shell's Scaffold, which then keeps snackbars above it
 * instead of on top of it.
 */
@Stable
class AddButtonState {
    var action by mutableStateOf<AddAction?>(null)
}

val LocalAddButtonState = staticCompositionLocalOf<AddButtonState> { error("No AddButtonState provided") }

/** Publishes [action] while the calling screen is shown. */
@Composable
fun PublishAddButton(action: AddAction) {
    val state = LocalAddButtonState.current
    val current by rememberUpdatedState(action)
    SideEffect { state.action = action }
    DisposableEffect(state) {
        // Only clear what this screen published: the next tab may already have set its own.
        onDispose { if (state.action === current) state.action = null }
    }
}

/** The Scaffold's floatingActionButton slot. */
@Composable
fun AddButtonSlot(state: AddButtonState) {
    val action = state.action
    // Keep the last action while the exit animation plays.
    val shown = remember { mutableStateOf<AddAction?>(null) }
    if (action != null) shown.value = action
    AnimatedVisibility(
        visible = action?.visible == true,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut(),
    ) {
        shown.value?.let { AddButton(onClick = it.onClick, contentDescription = it.description) }
    }
}
