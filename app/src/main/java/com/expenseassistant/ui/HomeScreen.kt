package com.expenseassistant.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    notificationAccessGranted: Boolean,
    accessibilityGranted: Boolean,
    onCategoryChange: (Long, Category) -> Unit,
    onDelete: (Long) -> Unit,
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Expense Assistant") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
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
                    "Transactions this month",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            if (state.transactions.isEmpty()) {
                item {
                    Text(
                        "Nothing captured yet. Make a UPI payment and it will appear here automatically.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            items(state.transactions, key = { it.id }) { transaction ->
                TransactionRow(
                    transaction = transaction,
                    onCategoryChange = { onCategoryChange(transaction.id, it) },
                    onDelete = { onDelete(transaction.id) },
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(state: HomeUiState) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Spent this month", style = MaterialTheme.typography.labelMedium)
            Text(formatMinor(state.monthSpendMinor), style = MaterialTheme.typography.headlineMedium)
            Text(
                "Received ${formatMinor(state.monthIncomeMinor)} · ${state.transactions.size} transactions",
                style = MaterialTheme.typography.bodySmall,
            )
            if (state.needsReviewCount > 0) {
                Text(
                    "${state.needsReviewCount} need a category check",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun CategoryBreakdown(breakdown: List<Pair<Category, Long>>) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Where it went", style = MaterialTheme.typography.titleSmall)
            breakdown.take(6).forEach { (category, amount) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(category.displayName, style = MaterialTheme.typography.bodyMedium)
                    Text(formatMinor(amount), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(
    transaction: TransactionEntity,
    onCategoryChange: (Category) -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val isDebit = transaction.direction == Direction.DEBIT

    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    transaction.merchant,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    (if (isDebit) "- " else "+ ") + formatMinor(transaction.amountMinor),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isDebit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                "${transaction.sourceApp} · ${formatTimestamp(transaction.occurredAt)}",
                style = MaterialTheme.typography.bodySmall,
            )
            HorizontalDivider()
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { menuExpanded = true }) {
                    Text(transaction.category.displayName)
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    Category.entries.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.displayName) },
                            onClick = {
                                menuExpanded = false
                                onCategoryChange(category)
                            },
                        )
                    }
                }
                TextButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}
