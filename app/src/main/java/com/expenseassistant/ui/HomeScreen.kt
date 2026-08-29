package com.expenseassistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expenseassistant.data.model.Category
import com.expenseassistant.data.model.Direction
import com.expenseassistant.data.model.TransactionEntity
import com.expenseassistant.ui.category.CategoryBadge
import com.expenseassistant.ui.category.CategoryDot
import com.expenseassistant.ui.category.CategoryPickerSheet

@Composable
fun HomeScreen(
    state: HomeUiState,
    notificationAccessGranted: Boolean,
    accessibilityGranted: Boolean,
    onCategoryChange: (Long, Category) -> Unit,
    onDelete: (Long) -> Unit,
    onOpenTransaction: (Long) -> Unit,
    categoryFilter: Category? = null,
    tagFilter: String? = null,
    onClearFilter: () -> Unit = {},
    transactionOverride: List<TransactionEntity>? = null,
    modifier: Modifier = Modifier,
) {
    var editing by remember { mutableStateOf<TransactionEntity?>(null) }
    val hasFilter = categoryFilter != null || tagFilter != null
    val filterLabel = categoryFilter?.displayName ?: tagFilter?.let { "#$it" }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            PermissionsCard(notificationAccessGranted, accessibilityGranted)
        }
        item { SummaryCard(state) }

        if (state.spendByCategory.isNotEmpty()) {
            item { CategoryBreakdown(state.spendByCategory) }
        }

        item {
            Text(
                if (filterLabel == null) "Recent transactions" else "$filterLabel transactions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        if (state.transactions.isEmpty()) {
            item {
                Text(
                    if (filterLabel == null) "No transactions this month." else "No transactions found.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        items(
            (transactionOverride ?: state.transactions).filter { tx ->
                (categoryFilter == null || tx.category == categoryFilter) &&
                    (tagFilter == null || tx.tags.any { it.equals(tagFilter, ignoreCase = true) })
            },
            key = { it.id },
        ) { transaction ->
            TransactionRow(
                transaction = transaction,
                onEditCategory = { editing = transaction },
                onDelete = { onDelete(transaction.id) },
                onClick = { onOpenTransaction(transaction.id) },
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
            onSelect = { category ->
                onCategoryChange(transaction.id, category)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun SummaryCard(state: HomeUiState) {
    val hero = rememberHeroGradient()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(hero.brush)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                "Spent this month",
                style = MaterialTheme.typography.labelMedium,
                color = hero.onGradientMuted,
            )
            Text(
                formatMinor(state.spendMinor),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = hero.onGradient,
            )
            Text(
                "Received ${formatMinor(state.incomeMinor)} \u00b7 ${state.transactions.size} transactions",
                style = MaterialTheme.typography.bodySmall,
                color = hero.onGradientMuted,
            )
            if (state.needsReviewCount > 0) {
                Text(
                    "${state.needsReviewCount} need a category check",
                    style = MaterialTheme.typography.bodySmall,
                    color = hero.onGradient,
                )
            }
        }
    }
}

@Composable
private fun CategoryBreakdown(breakdown: List<Pair<Category, Long>>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(
            Modifier
                .background(rememberSoftGradient())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Where it went", style = MaterialTheme.typography.titleSmall)
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
                    Text(formatMinor(amount), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(
    transaction: TransactionEntity,
    onEditCategory: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
) {
    val isDebit = transaction.direction == Direction.DEBIT

    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(
            Modifier
                .background(rememberSoftGradient())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CategoryBadge(transaction.category, size = 42.dp)
                Column(Modifier.weight(1f)) {
                    Text(transaction.merchant, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${transaction.sourceApp} · ${formatTimestamp(transaction.occurredAt)}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(
                    (if (isDebit) "- " else "+ ") + formatMinor(transaction.amountMinor),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isDebit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
            HorizontalDivider()
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onEditCategory) {
                    Icon(Icons.Filled.Edit, contentDescription = null)
                    Text("  ${transaction.category.displayName}")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete transaction")
                }
            }
        }
    }
}
