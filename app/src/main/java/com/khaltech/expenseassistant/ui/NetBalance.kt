package com.khaltech.expenseassistant.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Income minus spending for a totals card, signed and coloured so the direction reads at a glance:
 * red when more went out than came in, green otherwise.
 */
@Composable
fun NetBalance(incomeMinor: Long, spentMinor: Long, labelColor: Color, modifier: Modifier = Modifier) {
    val netMinor = incomeMinor - spentMinor
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            "NET Balance:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = labelColor,
        )
        Text(
            (if (netMinor < 0) "-" else "+") + formatMinor(kotlin.math.abs(netMinor)),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (netMinor < 0) SpendColor else IncomeColor,
        )
    }
}
