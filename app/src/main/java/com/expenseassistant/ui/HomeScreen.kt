package com.expenseassistant.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.expenseassistant.data.model.Category
import com.expenseassistant.data.model.Direction
import com.expenseassistant.data.model.TransactionEntity
import com.expenseassistant.data.repo.CustomCategoryOption
import com.expenseassistant.ui.category.CategoryBadge
import com.expenseassistant.ui.category.CategoryDot
import com.expenseassistant.ui.category.CategoryPickerSheet
import com.expenseassistant.ui.category.displayCategoryName

@OptIn(ExperimentalFoundationApi::class)

@Composable
fun HomeScreen(
    state: HomeUiState,
    notificationAccessGranted: Boolean,
    accessibilityGranted: Boolean,
    onCategoryChange: (Long, Category) -> Unit,
    onCategoryChangeCustom: (Long, String, String, String) -> Unit = { _, _, _, _ -> },
    customCategories: List<CustomCategoryOption> = emptyList(),
    summaryScope: SummaryScope = SummaryScope.MONTH,
    onSummaryScopeChange: (SummaryScope) -> Unit = {},
    onDelete: (Long) -> Unit,
    onOpenTransaction: (Long) -> Unit,
    categoryFilter: Category? = null,
    tagFilter: String? = null,
    needsReviewFilter: Boolean = false,
    onOpenNeedsReview: () -> Unit = {},
    onClearFilter: () -> Unit = {},
    transactionOverride: List<TransactionEntity>? = null,
    modifier: Modifier = Modifier,
) {
    var editing by remember { mutableStateOf<TransactionEntity?>(null) }
    val hasFilter = categoryFilter != null || tagFilter != null || needsReviewFilter
    val filterLabel = categoryFilter?.displayName
        ?: tagFilter?.let { "#$it" }
        ?: "Needs a category".takeIf { needsReviewFilter }

    val visible = (transactionOverride ?: state.transactions).filter { tx ->
        (categoryFilter == null || tx.category == categoryFilter) &&
            (tagFilter == null || tx.tags.any { it.equals(tagFilter, ignoreCase = true) }) &&
            (!needsReviewFilter || tx.needsCategoryReview)
    }
    val days = visible.groupBy { startOfDay(it.occurredAt) }
        .toList()
        .sortedByDescending { it.first }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            PermissionsCard(notificationAccessGranted, accessibilityGranted)
        }
        item { SectionHeader("Income & Expenditure") }
        item { SummaryCard(state, summaryScope, onSummaryScopeChange, onOpenNeedsReview) }

        if (state.spendByCategory.isNotEmpty()) {
            item { SectionHeader("Where it went") }
            item { CategoryBreakdown(state.spendByCategory) }
        }

        item {
            SectionHeader(if (filterLabel == null) "Latest activity" else "$filterLabel activity")
        }

        if (visible.isEmpty()) {
            item {
                Text(
                    if (filterLabel == null) "No transactions this month." else "No transactions found.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(days, key = { it.first }) { (dayStart, dayTransactions) ->
            DayGroupCard(
                dayStart = dayStart,
                transactions = dayTransactions,
                onOpenTransaction = onOpenTransaction,
                onEditCategory = { editing = it },
                onDelete = onDelete,
            )
        }

        if (hasFilter) {
            item {
                TextButton(onClick = onClearFilter, modifier = Modifier.fillMaxWidth()) {
                    Text("Show all recent transactions")
                }
            }
        }
    }

    editing?.let { transaction ->
        CategoryPickerSheet(
            merchant = transaction.merchant,
            selected = transaction.category,
            selectedCustomName = transaction.customCategoryName,
            customCategories = customCategories,
            onSelect = { category ->
                onCategoryChange(transaction.id, category)
                editing = null
            },
            onSelectCustom = { name, colorHex, iconKey ->
                onCategoryChangeCustom(transaction.id, name, colorHex, iconKey)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp),
    )
}

/** One card per day, matching how the reference app groups a day's spending together. */
@Composable
private fun DayGroupCard(
    dayStart: Long,
    transactions: List<TransactionEntity>,
    onOpenTransaction: (Long) -> Unit,
    onEditCategory: (TransactionEntity) -> Unit,
    onDelete: (Long) -> Unit,
) {
    val netMinor = transactions.sumOf {
        if (it.direction == Direction.DEBIT) -it.amountMinor else it.amountMinor
    }
    val ordered = transactions.sortedByDescending { it.occurredAt }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = CardElevation),
    ) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    formatDayHeader(dayStart),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                val dayColor = if (netMinor < 0) SpendColor else IncomeColor
                Text(
                    (if (netMinor < 0) "-" else "+") + formatMinor(kotlin.math.abs(netMinor)),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = dayColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(dayColor.copy(alpha = 0.14f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            ordered.forEach { transaction ->
                TransactionRow(
                    transaction = transaction,
                    onClick = { onOpenTransaction(transaction.id) },
                    onEditCategory = { onEditCategory(transaction) },
                    onDelete = { onDelete(transaction.id) },
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(
    state: HomeUiState,
    scope: SummaryScope,
    onScopeChange: (SummaryScope) -> Unit,
    onOpenNeedsReview: () -> Unit,
) {
    val hero = rememberHeroGradient()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = CardElevation),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(hero.brush)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                when (scope) {
                    SummaryScope.MONTH -> currentMonthYearName()
                    SummaryScope.YEAR -> yearToDateLabel()
                    SummaryScope.ALL -> scope.label
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = hero.onGradient,
            )
            ScopeToggle(scope = scope, onScopeChange = onScopeChange, hero = hero)
            val proportionTotal = (state.spendMinor + state.incomeMinor).coerceAtLeast(1)
            val spendFraction = (state.spendMinor.toFloat() / proportionTotal).coerceIn(0.04f, 0.96f)
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
            ) {
                Box(Modifier.weight(1f - spendFraction).fillMaxHeight().background(IncomeColor))
                Box(Modifier.weight(spendFraction).fillMaxHeight().background(SpendColor))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(
                        "INCOME",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = IncomeColor,
                    )
                    Text(
                        formatMinor(state.incomeMinor),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = hero.onGradient,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "EXPENDITURE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = SpendColor,
                    )
                    Text(
                        formatMinor(state.spendMinor),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = hero.onGradient,
                    )
                }
            }
            val netMinor = state.incomeMinor - state.spendMinor
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(hero.onGradient.copy(alpha = 0.07f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Net Balance",
                    style = MaterialTheme.typography.bodyMedium,
                    color = hero.onGradientMuted,
                )
                Text(
                    (if (netMinor < 0) "-" else "") + formatMinor(kotlin.math.abs(netMinor)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (netMinor < 0) SpendColor else IncomeColor,
                )
            }
            if (state.needsReviewCount > 0) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenNeedsReview)
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        "${state.needsReviewCount} need a category check",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = hero.onGradient,
                    )
                    Text(
                        "Review \u203a",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = hero.onGradientMuted,
                    )
                }
            }
        }
    }
}

/** Segmented pill letting the summary switch between this month, this year and everything. */
@Composable
private fun ScopeToggle(
    scope: SummaryScope,
    onScopeChange: (SummaryScope) -> Unit,
    hero: HeroGradient,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(hero.onGradient.copy(alpha = 0.07f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        SummaryScope.entries.forEach { option ->
            val isSelected = option == scope
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) hero.onGradient.copy(alpha = 0.16f) else Color.Transparent)
                    .clickable { onScopeChange(option) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    option.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) hero.onGradient else hero.onGradientMuted,
                )
            }
        }
    }
}

@Composable
private fun CategoryBreakdown(breakdown: List<Pair<Category, Long>>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = CardElevation),
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            breakdown.take(6).forEach { (category, amount) ->
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CategoryDot(category)
                    Text(
                        category.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        formatMinor(amount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TransactionRow(
    transaction: TransactionEntity,
    onEditCategory: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
) {
    val isDebit = transaction.direction == Direction.DEBIT

    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onEditCategory)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CategoryBadge(transaction, size = 44.dp)
        Column(Modifier.weight(1f)) {
            Text(
                transaction.merchant,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                transaction.displayCategoryName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                (if (isDebit) "-" else "+") + formatMinor(transaction.amountMinor),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (isDebit) SpendColor else IncomeColor,
            )
            Text(
                formatTimeOnly(transaction.occurredAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
