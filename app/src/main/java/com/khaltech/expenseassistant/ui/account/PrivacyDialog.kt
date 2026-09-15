package com.khaltech.expenseassistant.ui.account

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Published from docs/privacy-policy/ by GitHub Pages; the same URL is given in Play Console. */
private const val PRIVACY_POLICY_URL = "https://khalstudios.github.io/MyExpenseAssistant/privacy-policy/"

/**
 * The in-app privacy summary. Mirrors docs/privacy-policy/index.md, the full policy published for
 * the store listing, and links to it.
 */
@Composable
fun PrivacyDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("How your data is handled") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Section(
                    "Nothing leaves this phone",
                    "Kahan Gaya Paisa has no internet permission, so it cannot upload your " +
                        "transactions, contacts or notifications. There are no ads, no analytics " +
                        "and no third-party tracking.",
                )
                Section(
                    "What it reads",
                    "Payment notifications and bank SMS alerts, once you enable notification access, " +
                        "for the amount, payee and time. Contact names, if you turn that on, so " +
                        "transfers show a name instead of a number.",
                )
                Section(
                    "What it stores",
                    "Your transactions, budgets, learned categories, profile details and settings, " +
                        "all in this app's private storage on this device.",
                )
                Section(
                    "Backups and exports",
                    "Backup and CSV files are written to the folder you pick. If that folder syncs " +
                        "to a cloud service, that copy is then handled by that service.",
                )
                Section(
                    "Deleting your data",
                    "Use Delete all transactions below, or uninstall the app to remove everything.",
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        // The app has no internet permission; the browser loads the page.
        dismissButton = {
            TextButton(
                onClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL)))
                    }
                },
            ) { Text("Full privacy policy") }
        },
    )
}

@Composable
private fun Section(title: String, body: String) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
