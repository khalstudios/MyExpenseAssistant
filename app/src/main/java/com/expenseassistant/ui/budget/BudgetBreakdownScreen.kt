package com.expenseassistant.ui.budget

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expenseassistant.data.model.Category
import com.expenseassistant.ui.CardElevation
import com.expenseassistant.ui.category.CategoryBadge
import com.expenseassistant.ui.category.color
import com.expenseassistant.ui.formatMinor
import com.expenseassistant.ui.insights.AnalyticsUiState
import com.expenseassistant.ui.insights.BudgetProgress
import com.expenseassistant.ui.rememberSoftGradient

@Composable
fun BudgetBreakdownScreen(
    state: AnalyticsUiState,
    onManageBudgets: () -> Unit,
    onOpenCategory: (Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "Budget overview",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        item {
            BudgetOverviewCard(
                overall = state.overallBudget,
                categories = state.categoryBudgets,
            )
        }
        item {
            BudgetSummaryCard(
                overall = state.overallBudget,
                categories = state.categoryBudgets,
                onManage = onManageBudgets,
                onOpenCategory = onOpenCategory,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun BudgetOverviewCard(
    overall: BudgetProgress?,
    categories: List<BudgetProgress>,
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                OverallUsageRing(
                    progress = overall,
                    modifier = Modifier.weight(0.9f),
                )
                CategoryBudgetBars(
                    categories = categories,
                    modifier = Modifier.weight(1.1f),
                )
            }
        }
    }
}

@Composable
private fun OverallUsageRing(
    progress: BudgetProgress?,
    modifier: Modifier = Modifier,
) {
    val sweepProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 700),
        label = "budgetRingSweep",
    )
    val accent = when {
        progress == null -> MaterialTheme.colorScheme.primary
        progress.isOverBudget -> MaterialTheme.colorScheme.error
        progress.isAheadOfPace -> warningColor
        else -> onTrackColor
    }
    val fraction = progress?.fraction?.coerceIn(0f, 1f) ?: 0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxWidth().aspectRatio(1f)) {
            val thickness = size.minDimension * 0.14f
            val inset = thickness / 2f
            val arcSize = Size(size.width - thickness, size.height - thickness)
            drawArc(
                color = accent.copy(alpha = 0.16f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = thickness, cap = StrokeCap.Round),
            )
            drawArc(
                color = accent,
                startAngle = -90f,
                sweepAngle = fraction * 360f * sweepProgress,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = thickness, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (progress == null) "No limit" else "Used",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                if (progress == null) "0%" else "${(progress.fraction * 100).toInt()}%",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                progress?.let { formatMinor(it.remainingMinor) + " left" } ?: "Set a budget",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = accent,
            )
        }
    }
}

@Composable
private fun CategoryBudgetBars(
    categories: List<BudgetProgress>,
    modifier: Modifier = Modifier,
) {
    val visibleCategories = categories.take(4)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (visibleCategories.isEmpty()) {
            Text(
                "Set category budgets to see usage trends here.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        visibleCategories.forEach { progress ->
            val category = progress.category ?: return@forEach
            val accent = when {
                progress.isOverBudget -> MaterialTheme.colorScheme.error
                progress.isAheadOfPace -> warningColor
                else -> category.color
            }
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CategoryBadge(category, size = 34.dp)
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            category.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "${(progress.fraction * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = accent,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progress.fraction.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = accent,
                        trackColor = accent.copy(alpha = 0.15f),
                    )
                }
            }
        }
    }
}

private val onTrackColor = Color(0xFF2E7D32)
private val warningColor = Color(0xFFEF6C00)