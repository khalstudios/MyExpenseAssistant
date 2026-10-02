package com.khaltech.expenseassistant.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.withTimeoutOrNull

/** Long enough to read a two- or three-word confirmation; Material's shortest is four seconds. */
private const val MessageVisibleMillis = 1_000L

/** Shows [message] in place of any message still showing, for about a second. */
suspend fun SnackbarHostState.showBriefly(message: String) {
    currentSnackbarData?.dismiss()
    // Cancelling the wait dismisses the bar.
    withTimeoutOrNull(MessageVisibleMillis) { showSnackbar(message) }
}

/**
 * Dismisses the message on any touch inside this layout. The touch is only observed, never consumed,
 * so it still reaches whatever was tapped.
 */
fun Modifier.dismissMessageOnTouch(messages: SnackbarHostState): Modifier = pointerInput(messages) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        messages.currentSnackbarData?.dismiss()
    }
}
