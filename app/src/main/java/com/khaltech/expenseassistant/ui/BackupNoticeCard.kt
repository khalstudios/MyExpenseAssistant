package com.khaltech.expenseassistant.ui

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Suggests automatic backups. For a free user it says up front that they are part of Pro, so
 * "Upgrade" leads to the paywall rather than to a setup the user was not told they could not finish.
 */
@Composable
fun BackupNoticeCard(
    onEnable: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    needsPro: Boolean = false,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Turn on automatic backups", style = MaterialTheme.typography.titleMedium)
            Text(
                if (needsPro) {
                    "Save a copy of your transactions to a folder you choose, on a schedule, so you don't lose data if something goes wrong. Automatic backups are part of Pro."
                } else {
                    "Save a copy of your transactions to a folder you choose, on a schedule, so you don't lose data if something goes wrong."
                },
                style = MaterialTheme.typography.bodySmall,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Not now") }
                TextButton(onClick = onEnable) { Text(if (needsPro) "Upgrade to Pro" else "Set up") }
            }
        }
    }
}
