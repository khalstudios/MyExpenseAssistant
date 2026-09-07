package com.expenseassistant.ui

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.expenseassistant.service.PermissionStatus

@Composable
fun PermissionsCard(
    notificationAccessGranted: Boolean,
    modifier: Modifier = Modifier,
) {
    if (notificationAccessGranted) return
    val context = LocalContext.current
    var showDisclosure by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Finish setup", style = MaterialTheme.typography.titleMedium)
            PermissionRow(
                title = "Notification access",
                description = "Required to read payment confirmations from GPay, PhonePe and Paytm.",
            ) { showDisclosure = true }
        }
    }

    // Consent is asked for here, before the user reaches the system screen that grants it.
    if (showDisclosure) {
        DisclosureDialog(
            disclosure = Disclosures.Notifications,
            onAccept = {
                showDisclosure = false
                context.openSettings(PermissionStatus.notificationAccessIntent())
            },
            onDismiss = { showDisclosure = false },
        )
    }
}

@Composable
private fun PermissionRow(title: String, description: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
        TextButton(onClick = onClick) { Text("Enable") }
    }
}

private fun Context.openSettings(intent: android.content.Intent) {
    runCatching { startActivity(intent) }
}
