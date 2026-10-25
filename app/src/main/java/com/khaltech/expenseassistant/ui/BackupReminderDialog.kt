package com.khaltech.expenseassistant.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/** Every few days until automatic backups are on: data lives only on this phone, so it is worth a copy. */
@Composable
fun BackupReminderDialog(
    needsPro: Boolean,
    onEnable: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Turn on automatic backups") },
        text = {
            Text(
                if (needsPro) {
                    "Save a copy of your transactions to a folder you choose, on a schedule, so you don't lose data if something goes wrong. Automatic backups are part of Pro."
                } else {
                    "Save a copy of your transactions to a folder you choose, on a schedule, so you don't lose data if something goes wrong."
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = { TextButton(onClick = onEnable) { Text(if (needsPro) "Upgrade to Pro" else "Set up") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } },
    )
}
