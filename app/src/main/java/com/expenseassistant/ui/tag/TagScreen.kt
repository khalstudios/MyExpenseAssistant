package com.expenseassistant.ui.tag

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expenseassistant.data.model.Direction
import com.expenseassistant.data.model.TransactionEntity
import com.expenseassistant.ui.category.CategoryBadge
import com.expenseassistant.ui.category.displayCategoryName
import com.expenseassistant.ui.CardElevation
import com.expenseassistant.ui.formatMinor
import com.expenseassistant.ui.formatTimestamp
import com.expenseassistant.ui.rememberHeroGradient
import com.expenseassistant.ui.rememberSoftGradient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagScreen(
    tag: String,
    transactions: List<TransactionEntity>,
    onBack: () -> Unit,
    onOpenTransaction: (Long) -> Unit,
) {
    val spentMinor = transactions.filter { it.direction == Direction.DEBIT }.sumOf { it.amountMinor }
    val receivedMinor = transactions.filter { it.direction == Direction.CREDIT }.sumOf { it.amountMinor }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("#$tag") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { TagTotalsCard(spentMinor, receivedMinor, transactions.size) }

            item {
                Text(
                    "Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            if (transactions.isEmpty()) {
                item {
                    Text(
                        "No transactions are tagged #$tag yet.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            items(transactions, key = { it.id }) { transaction ->
                TagTransactionRow(transaction = transaction, onClick = { onOpenTransaction(transaction.id) })
            }
        }
    }
}

@Composable
private fun TagTotalsCard(spentMinor: Long, receivedMinor: Long, count: Int) {
    val hero = rememberHeroGradient()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = CardElevation),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(hero.brush)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                "$count ${if (count == 1) "transaction" else "transactions"}",
                style = MaterialTheme.typography.labelMedium,
                color = hero.onGradientMuted,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Spent", style = MaterialTheme.typography.labelMedium, color = hero.onGradientMuted)
                    Text(
                        formatMinor(spentMinor),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = hero.onGradient,
                    )
                }
                Column {
                    Text("Received", style = MaterialTheme.typography.labelMedium, color = hero.onGradientMuted)
                    Text(
                        formatMinor(receivedMinor),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = hero.onGradient,
                    )
                }
            }
        }
    }
}

@Composable
private fun TagTransactionRow(transaction: TransactionEntity, onClick: () -> Unit) {
    val isDebit = transaction.direction == Direction.DEBIT
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = CardElevation),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(rememberSoftGradient())
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CategoryBadge(transaction, size = 42.dp)
            Column(Modifier.weight(1f)) {
                Text(transaction.merchant, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${transaction.displayCategoryName} \u00b7 ${formatTimestamp(transaction.occurredAt)}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                (if (isDebit) "- " else "+ ") + formatMinor(transaction.amountMinor),
                style = MaterialTheme.typography.titleSmall,
                color = if (isDebit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
        }
    }
}
