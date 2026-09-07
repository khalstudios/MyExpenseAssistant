package com.expenseassistant.ui

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
        summary = "This is how Expense Assistant records spending without you typing every payment in.",
        points = listOf(
            DisclosurePoint(
                "What it reads",
                "Notifications posted by payment and banking apps: the amount, the merchant or payee, and the time.",
            ),
            DisclosurePoint(
                "Why",
                "To add each payment to your transaction list automatically and sort it into a category.",
            ),
            DisclosurePoint(
                "Where it goes",
                "Onto this phone only. The app has no internet permission, so nothing can be uploaded or shared with anyone.",
            ),
            DisclosurePoint(
                "Your control",
                "You can turn this off at any time in Android Settings, and delete recorded transactions from Profile.",
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
                "Names and phone numbers from your contact list.",
            ),
            DisclosurePoint(
                "Why",
                "So a transfer shows a person's name instead of a phone number or UPI ID.",
            ),
            DisclosurePoint(
                "Where it goes",
                "A matched name is saved on this phone alongside the transaction. Your contacts are never uploaded.",
            ),
        ),
        confirmLabel = "Continue",
    )

    /**
     * Only present where on-screen capture ships; null in the Play build, which carries neither
     * the service nor any text describing it. See ScreenCaptureDisclosure per flavor.
     */
    val ScreenReading: Disclosure? = ScreenCaptureDisclosure
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
