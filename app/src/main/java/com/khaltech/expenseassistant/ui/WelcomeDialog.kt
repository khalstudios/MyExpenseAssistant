package com.khaltech.expenseassistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * The first thing a new user sees: what the app does, and one button to the only setup step that
 * matters. The full walkthrough stays under Help, so features are explained once there is data.
 */
@Composable
fun WelcomeDialog(
    captureEnabled: Boolean,
    onSetUpCapture: () -> Unit,
    onDismiss: () -> Unit,
) {
    val hero = rememberHeroGradient()

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.fillMaxWidth()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(hero.brush)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Text(
                        "Welcome to Kahan Gaya Paisa",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = hero.onGradient,
                    )
                    Text(
                        "Your spending, recorded on its own.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = hero.onGradient,
                    )
                }

                Column(
                    Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    WelcomeLine(
                        "Payments are picked up from GPay, PhonePe, Paytm and your bank's SMS alerts, then sorted into categories.",
                    )
                    WelcomeLine("Everything stays on this phone. Nothing is uploaded anywhere.")
                    Text(
                        "The full tour is under Help › Tutorial whenever you want it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (captureEnabled) {
                        Button(onClick = onDismiss) { Text("Get started") }
                    } else {
                        TextButton(onClick = onDismiss) { Text("Skip for now") }
                        Button(onClick = onSetUpCapture) { Text("Set up automatic capture") }
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeLine(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier
                .padding(top = 7.dp)
                .size(6.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
        )
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
