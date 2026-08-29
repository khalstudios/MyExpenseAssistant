package com.expenseassistant.ui.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expenseassistant.ui.category.CategoryBadge
import com.expenseassistant.ui.formatMinor
import com.expenseassistant.ui.insights.BudgetProgress
import com.expenseassistant.ui.rememberSoftGradient

private val onTrackColor = Color(0xFF2E7D32)
private val warningColor = Color(0xFFEF6C00)

@Composable
fun BudgetSummaryCard(
    overall: BudgetProgress?,
    categories: List<BudgetProgress>,
    onManage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            Modifier
                .background(rememberSoftGradient())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Savings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        "  Budget",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                TextButton(onClick = onManage) { Text("Manage") }
            }

            if (overall == null && categories.isEmpty()) {
                Text(
                    "No budgets set. Add a monthly limit to track how much is left.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            overall?.let { BudgetRow(it, isOverall = true) }
            categories.forEach { BudgetRow(it, isOverall = false) }
        }
    }
}

@Composable
fun BudgetRow(progress: BudgetProgress, isOverall: Boolean) {
    val accent = when {
        progress.isOverBudget -> MaterialTheme.colorScheme.error
        progress.isAheadOfPace -> warningColor
        else -> onTrackColor
    }
    val status = when {
        progress.isOverBudget -> "Over by ${formatMinor(-progress.remainingMinor)}"
        progress.isAheadOfPace -> "${formatMinor(progress.remainingMinor)} left · spending fast"
        else -> "${formatMinor(progress.remainingMinor)} left · on track"
    }

    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (isOverall || progress.category == null) {
            Icon(
                Icons.Filled.Savings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(38.dp).padding(7.dp),
            )
        } else {
            CategoryBadge(progress.category, size = 38.dp)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    progress.category?.displayName ?: "All spending",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    "${formatMinor(progress.spentMinor)} / ${formatMinor(progress.limitMinor)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            LinearProgressIndicator(
                progress = { progress.fraction.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = accent,
                trackColor = accent.copy(alpha = 0.15f),
            )
            Text(status, style = MaterialTheme.typography.bodySmall, color = accent)
        }
    }
}
