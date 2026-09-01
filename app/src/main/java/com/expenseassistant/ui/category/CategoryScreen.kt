package com.expenseassistant.ui.category

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import com.expenseassistant.data.repo.CustomCategoryOption
import com.expenseassistant.ui.CardElevation
import com.expenseassistant.ui.DayGroupCard
import com.expenseassistant.ui.IncomeColor
import com.expenseassistant.ui.SpendColor
import com.expenseassistant.ui.formatMinor
import com.expenseassistant.ui.rememberHeroGradient
import com.expenseassistant.ui.startOfDay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CategoryScreen(
    category: Category,
    transactions: List<TransactionEntity>,
    onBack: () -> Unit,
    onOpenTransaction: (Long) -> Unit,
    onCategoryChange: (Long, Category) -> Unit,
    onCategoryChangeCustom: (Long, String, String, String) -> Unit = { _, _, _, _ -> },
    customCategories: List<CustomCategoryOption> = emptyList(),
    onDelete: (Long) -> Unit = {},
) {
    var editing by remember { mutableStateOf<TransactionEntity?>(null) }

    val debits = transactions.filter { it.direction == Direction.DEBIT }
    val credits = transactions.filter { it.direction == Direction.CREDIT }
    val spentMinor = debits.sumOf { it.amountMinor }
    val incomeMinor = credits.sumOf { it.amountMinor }

    val days = transactions.groupBy { startOfDay(it.occurredAt) }
        .toList()
        .sortedByDescending { it.first }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CategoryBadge(category, size = 36.dp)
                        Text(category.displayName, fontWeight = FontWeight.Bold)
                    }
                },
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                CategoryTotalsCard(
                    spentMinor = spentMinor,
                    incomeMinor = incomeMinor,
                    count = transactions.size,
                )
            }

            item {
                Text(
                    "Transactions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            if (transactions.isEmpty()) {
                item {
                    Text(
                        "No transactions found for ${category.displayName}.",
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
        }
    }

    editing?.let { transaction ->
        CategoryPickerSheet(
            merchant = transaction.merchant,
            selected = transaction.category,
            selectedCustomName = transaction.customCategoryName,
            customCategories = customCategories,
            onSelect = { newCat ->
                onCategoryChange(transaction.id, newCat)
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
private fun CategoryTotalsCard(
    spentMinor: Long,
    incomeMinor: Long,
    count: Int,
) {
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "$count ${if (count == 1) "transaction" else "transactions"}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = hero.onGradientMuted,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(
                        "SPENDING",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = SpendColor,
                    )
                    Text(
                        formatMinor(spentMinor),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = hero.onGradient,
                    )
                }
                if (incomeMinor > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "INCOME",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = IncomeColor,
                        )
                        Text(
                            formatMinor(incomeMinor),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = hero.onGradient,
                        )
                    }
                }
            }
        }
    }
}
