package com.khaltech.expenseassistant.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.khaltech.expenseassistant.service.PermissionStatus

/** What to call the phone's SMS app in copy, from the package Android reports as the default. */
@Composable
private fun rememberSmsAppName(): String {
    val context = LocalContext.current
    return remember {
        when (PermissionStatus.defaultSmsPackage(context)) {
            "com.google.android.apps.messaging", "com.samsung.android.messaging", "com.android.mms" -> "Messages"
            "com.truecaller" -> "Truecaller"
            else -> "your SMS app"
        }
    }
}

/** Shown once, right after notification access is granted: bank SMS also need the SMS app's own notifications. */
@Composable
fun SmsNotificationDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    val smsApp = rememberSmsAppName()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Keep SMS notifications on 📩") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Bank alerts reach the app as notifications from $smsApp. If they're turned off, " +
                        "payments your bank texts you about can't be recorded.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "Silent notifications are fine. Only ones that are switched off, for the app or for your " +
                        "bank's conversation, are missed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = onOpenSettings) { Text("Check settings") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

/** Shown on Home when capture has run for a few days without a single SMS-app notification arriving. */
@Composable
fun SmsNoticeCard(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val smsApp = rememberSmsAppName()
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("No bank SMS alerts yet 📩", style = MaterialTheme.typography.titleMedium)
            Text(
                "Nothing has arrived from $smsApp since capture was turned on. If your bank texts you about " +
                    "payments, check that its notifications aren't switched off. Silent is fine.",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Dismiss") }
                TextButton(onClick = onOpenSettings) { Text("Check settings") }
            }
        }
    }
}
