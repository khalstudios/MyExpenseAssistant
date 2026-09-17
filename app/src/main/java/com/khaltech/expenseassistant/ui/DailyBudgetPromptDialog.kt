package com.khaltech.expenseassistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Shown once on the first opening of each day: sets the day's budget and reports how the month is
 * tracking. [suggestedMinor] seeds the field with whatever today is currently measured against.
 */
@Composable
fun DailyBudgetPromptDialog(
    suggestedMinor: Long,
    status: SpendingStatus?,
    onSave: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    // The dialog opens before the stored budget has loaded, so seed the field when it arrives —
    // but never overwrite what the user has started typing.
    var edited by remember { mutableStateOf(false) }
    LaunchedEffect(suggestedMinor) {
        if (!edited && suggestedMinor > 0) text = (suggestedMinor / 100).toString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Budget for today") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                status?.let { MonthTrendNote(it) }
                Text(
                    "How much do you want to spend today?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { input ->
                        edited = true
                        text = input.filter { it.isDigit() || it == '.' }
                    },
                    label = { Text("Daily limit") },
                    prefix = { Text("₹ ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(text.toMinorUnits()) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } },
    )
}

/** The same reading the home banner gives, so the day starts with the month's context. */
@Composable
private fun MonthTrendNote(status: SpendingStatus) {
    val accent = when (status.mood) {
        SpendingMood.GOOD -> IncomeColor
        SpendingMood.WATCH -> WarningColor
        SpendingMood.OVER -> SpendColor
    }
    val icon = when (status.mood) {
        SpendingMood.GOOD -> Icons.Filled.CheckCircle
        SpendingMood.WATCH -> Icons.Filled.TrendingUp
        SpendingMood.OVER -> Icons.Filled.WarningAmber
    }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.12f).compositeOver(MaterialTheme.colorScheme.surface))
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                status.headline,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
            Text(
                status.detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
