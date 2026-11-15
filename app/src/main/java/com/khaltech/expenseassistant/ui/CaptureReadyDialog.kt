package com.khaltech.expenseassistant.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/**
 * Shown each time notification access has just been turned on, once any follow-up
 * prompts are out of the way: capture is ready, and the quickest way to trust it is to watch it work.
 */
@Composable
fun CaptureReadyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("All set. You're good to go! 🎉") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Payments from GPay, PhonePe, Paytm and your bank's alerts will now be recorded on their own.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "Try it: make a payment and come back. It'll show up under Latest activity within a few seconds.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Got it") } },
    )
}
