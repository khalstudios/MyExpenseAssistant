package com.expenseassistant.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.SouthEast
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expenseassistant.ui.budget.BudgetSummaryCard
import com.expenseassistant.ui.category.CategoryBadge
import com.expenseassistant.ui.CardElevation
import com.expenseassistant.ui.formatMinor
import com.expenseassistant.ui.rememberSoftGradient
import com.expenseassistant.data.model.Category
import com.expenseassistant.data.repo.TagUsage
import com.expenseassistant.recurring.RecurringExpense
import kotlin.math.abs

@Composable
fun InsightsScreen(
    state: AnalyticsUiState,
    recurring: List<RecurringExpense>,
    tagUsage: List<TagUsage>,
    onRangeChange: (AnalyticsRange) -> Unit,
    onShiftPeriod: (Int) -> Unit,
    onJumpTo: (year: Int, monthIndex: Int) -> Unit,
    onResetToCurrent: () -> Unit,
    onManageBudgets: () -> Unit,
    onOpenCategory: (Category) -> Unit,
    onOpenTag: (String) -> Unit,
    onOpenNeedsReview: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            PeriodNavigator(
                label = state.periodLabel,
                range = state.range,
                canGoForward = state.canGoForward,
                isCurrentPeriod = state.isCurrentPeriod,
                onRangeChange = onRangeChange,
                onShift = onShiftPeriod,
                onJumpTo = onJumpTo,
                onResetToCurrent = onResetToCurrent,
            )
        }
        item { SpendChartCard(state) }
        if (state.slices.isNotEmpty()) {
            item { CategoryListCard(state, onOpenCategory) }
        }
        if (tagUsage.isNotEmpty()) {
            item { TagsCard(tagUsage, onOpenTag) }
        }
        if (state.range == AnalyticsRange.MONTH) {
            item {
                BudgetSummaryCard(
                    overall = state.overallBudget,
                    categories = state.categoryBudgets,
                    onManage = onManageBudgets,
                    onOpenCategory = onOpenCategory,
                )
            }
        }
        item { StatGrid(state) }
        item { ComparisonCard(state) }
        if (recurring.isNotEmpty()) {
            item { RecurringCard(recurring) }
        }
        state.topMerchant?.let { (merchant, amount) ->
            item { TopMerchantCard(merchant, amount) }
        }
        state.largestTransaction?.let { transaction ->
            item {
                HighlightCard(
                    icon = Icons.Filled.NorthEast,
                    title = "Largest single expense",
                    primary = formatMinor(transaction.amountMinor),
                    secondary = transaction.merchant,
                    badge = { CategoryBadge(transaction, size = 40.dp) },
                )
            }
        }
        if (state.needsReviewCount > 0) {
            item { ReviewNudgeCard(state.needsReviewCount, onOpenNeedsReview) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsCard(tags: List<TagUsage>, onOpenTag: (String) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(rememberSoftGradient())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    "  Tags",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                "Tap a tag to see everything tagged with it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                tags.forEach { usage ->
                    AssistChip(
                        onClick = { onOpenTag(usage.tag) },
                        label = { Text("#${usage.tag} \u00b7 ${usage.count}") },
                    )
                }
            }
        }
    }
}

@Composable
private fun SpendChartCard(state: AnalyticsUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = CardElevation),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(rememberSoftGradient())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Insights, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    "  Spending by category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (state.slices.isEmpty()) {
                EmptyChartPlaceholder()
            } else {
                CategoryPieChart(slices = state.slices, totalMinor = state.totalSpendMinor)
            }
        }
    }
}

@Composable
private fun CategoryListCard(state: AnalyticsUiState, onOpenCategory: (Category) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Category breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    formatMinor(state.totalSpendMinor),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            HorizontalDivider()
            CategorySpendList(state.slices, onOpenCategory)
        }
    }
}

@Composable
private fun StatGrid(state: AnalyticsUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Timeline,
                label = "Daily average",
                value = formatMinor(state.dailyAverageMinor),
            )
            StatTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.CalendarMonth,
                label = "Projected ${state.range.label.lowercase()}",
                value = formatMinor(state.projectedTotalMinor),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Receipt,
                label = "Transactions",
                value = state.transactionCount.toString(),
            )
            StatTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.CalendarMonth,
                label = "Days with spend",
                value = state.activeDays.toString(),
            )
        }
    }
}

@Composable
private fun StatTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
) {
    Card(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(rememberSoftGradient())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ComparisonCard(state: AnalyticsUiState) {
    val percent = state.periodOverPeriodPercent
    val isUp = (percent ?: 0) >= 0
    val accent = if (isUp) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)

    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(rememberSoftGradient())
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                if (isUp) Icons.Filled.NorthEast else Icons.Filled.SouthEast,
                contentDescription = null,
                tint = accent,
            )
            Column(Modifier.weight(1f)) {
                Text("Compared to ${Periods.comparisonLabel(state.range)}", style = MaterialTheme.typography.labelMedium)
                Text(
                    when (percent) {
                        null -> "No data from ${Periods.comparisonLabel(state.range)}"
                        else -> "${if (isUp) "+" else "-"}${abs(percent)}% ${if (isUp) "more" else "less"} spending"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (percent == null) MaterialTheme.colorScheme.onSurface else accent,
                )
                Text(
                    "Previous: ${formatMinor(state.previousTotalSpendMinor)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TopMerchantCard(merchant: String, amountMinor: Long) {
    HighlightCard(
        icon = Icons.Filled.Storefront,
        title = "Most spent at",
        primary = merchant,
        secondary = formatMinor(amountMinor),
    )
}

@Composable
private fun HighlightCard(
    icon: ImageVector,
    title: String,
    primary: String,
    secondary: String,
    badge: (@Composable () -> Unit)? = null,
) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(rememberSoftGradient())
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (badge != null) badge() else {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelMedium)
                Text(primary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    secondary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ReviewNudgeCard(count: Int, onClick: () -> Unit) {
    Card(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("$count transactions need a category", style = MaterialTheme.typography.titleSmall)
            Text(
                "Tap to review them \u2014 the app remembers each merchant for next time.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
