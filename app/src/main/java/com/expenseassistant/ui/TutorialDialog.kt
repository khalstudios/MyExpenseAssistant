package com.expenseassistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/** One page of the walkthrough: a headline idea plus the details behind it. */
private data class TutorialStep(
    val icon: ImageVector,
    val title: String,
    val summary: String,
    val points: List<String>,
)

private val TutorialSteps = listOf(
    TutorialStep(
        icon = Icons.Filled.AutoAwesome,
        title = "Welcome",
        summary = "Expense Assistant keeps a running record of your spending without you typing in every payment.",
        points = listOf(
            "It reads the payment confirmations your apps already show you, sorts them into categories and adds up the month for you.",
            "Everything stays on this phone. Nothing is uploaded anywhere.",
        ),
    ),
    TutorialStep(
        icon = Icons.Filled.Notifications,
        title = "Automatic capture",
        summary = "Payments arrive on their own once you grant access under Profile › Capture.",
        points = listOf(
            "Notification access lets the app read payment alerts from GPay, PhonePe and Paytm.",
            "Screen reading catches payments that never post a notification.",
            "Contact names turn person-to-person transfers into a real name instead of a number.",
        ),
    ),
    TutorialStep(
        icon = Icons.Filled.Home,
        title = "The Home tab",
        summary = "Your day-to-day view of what came in and what went out.",
        points = listOf(
            "Net Position compares income against expenditure; switch between this month, this year and all time.",
            "Where it went breaks the period down by category. Tap any category to see just those transactions.",
            "Latest activity groups the last three days by day. See more opens your full history.",
        ),
    ),
    TutorialStep(
        icon = Icons.Filled.Category,
        title = "Categories that learn",
        summary = "Each captured payment is categorised for you, and your corrections stick.",
        points = listOf(
            "Long-press any transaction to change its category.",
            "Correct a merchant once and future payments from it follow your choice.",
            "Anything the app was unsure about is collected under Needs review, so you can fix them in one pass.",
        ),
    ),
    TutorialStep(
        icon = Icons.Filled.Add,
        title = "Adding things yourself",
        summary = "Cash, and anything that slipped through, goes in with the + button.",
        points = listOf(
            "Set the amount, merchant, category, payment mode and date.",
            "Tap any transaction to open it, edit the details, or add tags like a trip or a project.",
        ),
    ),
    TutorialStep(
        icon = Icons.Filled.PieChart,
        title = "Insights",
        summary = "The Insights tab looks at longer stretches of time.",
        points = listOf(
            "Move between months and years to see how spending trends over time.",
            "Recurring payments surfaces the subscriptions and bills that repeat.",
            "Tag spend shows where your tagged spending is actually going.",
        ),
    ),
    TutorialStep(
        icon = Icons.Filled.Savings,
        title = "Budgets",
        summary = "Set a monthly limit overall and per category.",
        points = listOf(
            "Manage limits from the Budget tab, or Profile › Planning.",
            "Home and the Budget tab then show how much of each limit you have used so far.",
        ),
    ),
    TutorialStep(
        icon = Icons.Filled.Backup,
        title = "Keep your data safe",
        summary = "Because your data lives only on this phone, backups are worth setting up.",
        points = listOf(
            "Automatic backups write a dated file into a folder you choose, every 15 days or monthly.",
            "You can also back up, restore or export to CSV at any time from Profile › Your data.",
        ),
    ),
)

@Composable
fun TutorialDialog(onDismiss: () -> Unit) {
    var index by remember { mutableIntStateOf(0) }
    val step = TutorialSteps[index]
    val isLast = index == TutorialSteps.lastIndex
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            Modifier
                                .size(44.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                    CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                step.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Step ${index + 1} of ${TutorialSteps.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = hero.onGradientMuted,
                            )
                            Text(
                                step.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = hero.onGradient,
                            )
                        }
                    }
                    Text(
                        step.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = hero.onGradient,
                    )
                }

                Column(
                    Modifier
                        .heightIn(max = 260.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    step.points.forEach { point -> BulletLine(point) }
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TutorialSteps.indices.forEach { position ->
                        val selected = position == index
                        Box(
                            Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (selected) 8.dp else 6.dp)
                                .background(
                                    if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                    },
                                    CircleShape,
                                ),
                        )
                    }
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!isLast) {
                        TextButton(onClick = onDismiss) { Text("Skip") }
                    }
                    Spacer(Modifier.weight(1f))
                    if (index > 0) {
                        TextButton(onClick = { index-- }) { Text("Back") }
                    }
                    Button(onClick = { if (isLast) onDismiss() else index++ }) {
                        Text(if (isLast) "Done" else "Next")
                    }
                }
            }
        }
    }
}

@Composable
private fun BulletLine(text: String) {
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
