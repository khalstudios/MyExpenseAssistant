package com.khaltech.expenseassistant.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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

/**
 * A single sensitive-access disclosure: what is read, why, and where it goes.
 *
 * Shown before the user is sent to the system screen that grants the access, so consent is
 * given with the facts in hand rather than after the fact.
 */
data class Disclosure(
    val title: String,
    val summary: String,
    val points: List<DisclosurePoint>,
    val confirmLabel: String,
)

data class DisclosurePoint(val label: String, val detail: String)

object Disclosures {

    val Notifications = Disclosure(
        title = "Read payment notifications?",
        summary = "To record your spending automatically, Kahan Gaya Paisa reads notifications from payment " +
            "apps, banking apps and SMS apps such as Google Messages, Samsung Messages and Truecaller.",
        points = listOf(
            DisclosurePoint(
                "What it keeps",
                "Only completed payments: the alert's text and what it states, such as amount, payee, time, " +
                    "bank, last 4 digits of the account, reference number and balance. Personal messages, OTPs " +
                    "and promotions are discarded, never stored.",
            ),
            DisclosurePoint(
                "Where it goes",
                "It stays on this phone. Nothing is uploaded or shared.",
            ),
            DisclosurePoint(
                "Android's next screen",
                "It warns that the app could read all notifications. It ignores notifications from every other app.",
            ),
            DisclosurePoint(
                "Your control",
                "Turn this off any time in Android Settings, and delete transactions from Profile.",
            ),
        ),
        confirmLabel = "Continue",
    )

    val Contacts = Disclosure(
        title = "Match payments to your contacts?",
        summary = "Optional. The app works fully without this.",
        points = listOf(
            DisclosurePoint(
                "What it reads",
                "Contact names only, to find one that closely matches a payee. Phone numbers and other contact details are not read.",
            ),
            DisclosurePoint(
                "Why",
                "So a transfer shows a person's name instead of a UPI ID or account name.",
            ),
            DisclosurePoint(
                "Where it goes",
                "A matched name is saved on this phone alongside the payee, so the search is not repeated. Your contacts are never uploaded.",
            ),
        ),
        confirmLabel = "Continue",
    )
}

@Composable
fun DisclosureDialog(
    disclosure: Disclosure,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(disclosure.title) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(disclosure.summary, style = MaterialTheme.typography.bodyMedium)
                disclosure.points.forEach { point ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column {
                            Text(
                                point.label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                point.detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onAccept) { Text(disclosure.confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } },
    )
}
