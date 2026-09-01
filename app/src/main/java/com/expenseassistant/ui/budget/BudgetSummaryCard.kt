package com.expenseassistant.ui.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expenseassistant.data.model.Category
import com.expenseassistant.ui.category.CategoryBadge
import com.expenseassistant.ui.CardElevation
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
    onOpenCategory: ((Category) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = CardElevation),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
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

            overall?.let { OverallBudgetHero(it) }

            if (categories.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEachIndexed { index, progress ->
                        CategoryBudgetRow(progress = progress, onOpenCategory = onOpenCategory)
                        if (index < categories.lastIndex) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                modifier = Modifier.padding(vertical = 2.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverallBudgetHero(progress: BudgetProgress) {
    val accent = when {
        progress.isOverBudget -> MaterialTheme.colorScheme.error
        progress.isAheadOfPace -> warningColor
        else -> onTrackColor
    }
    val status = when {
        progress.isOverBudget -> "Over limit by ${formatMinor(-progress.remainingMinor)}"
        progress.isAheadOfPace -> "${formatMinor(progress.remainingMinor)} left · spending fast"
        else -> "${formatMinor(progress.remainingMinor)} left · on track"
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "All spending",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "${formatMinor(progress.spentMinor)} / ${formatMinor(progress.limitMinor)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        LinearProgressIndicator(
            progress = { progress.fraction.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = accent,
            trackColor = accent.copy(alpha = 0.15f),
        )
        Text(
            status,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = accent,
        )
    }
}

@Composable
private fun CategoryBudgetRow(
    progress: BudgetProgress,
    onOpenCategory: ((Category) -> Unit)?,
) {
    val accent = when {
        progress.isOverBudget -> MaterialTheme.colorScheme.error
        progress.isAheadOfPace -> warningColor
        else -> onTrackColor
    }
    val statusText = when {
        progress.isOverBudget -> "Over by ${formatMinor(-progress.remainingMinor)}"
        else -> "${formatMinor(progress.remainingMinor)} left"
    }

    val category = progress.category
    val rowModifier = if (category != null && onOpenCategory != null) {
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onOpenCategory(category) }
            .padding(vertical = 4.dp, horizontal = 4.dp)
    } else {
        Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 4.dp)
    }

    Column(rowModifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (category != null) {
                CategoryBadge(category, size = 36.dp)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    category?.displayName ?: "Category",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = accent,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatMinor(progress.spentMinor),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "of ${formatMinor(progress.limitMinor)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        LinearProgressIndicator(
            progress = { progress.fraction.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
            color = accent,
            trackColor = accent.copy(alpha = 0.15f),
        )
    }
}
