package com.expenseassistant.ui.account

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expenseassistant.service.ScreenCapture

/**
 * The in-app privacy summary. Mirrors PRIVACY_POLICY.md in the repository root, which is the
 * version published for the store listing.
 */
@Composable
fun PrivacyDialog(onDismiss: () -> Unit) {
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
                    "Expense Assistant has no internet permission, so it cannot upload your " +
                        "transactions, contacts or notifications. There are no ads, no analytics " +
                        "and no third-party tracking.",
                )
                Section(
                    "What it reads",
                    buildString {
                        append("Payment notifications, once you enable that access, for the amount, ")
                        append("payee and time. Contact names, if you turn that on, so transfers ")
                        append("show a name instead of a number.")
                        if (ScreenCapture.AVAILABLE) {
                            append(" Payment success screens in GPay, PhonePe, Paytm and BHIM, if ")
                            append("you enable screen reading.")
                        }
                    },
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
