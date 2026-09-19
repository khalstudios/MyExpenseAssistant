package com.khaltech.expenseassistant.ui.category

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
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
import com.khaltech.expenseassistant.data.model.Category
import com.khaltech.expenseassistant.data.model.Direction
import com.khaltech.expenseassistant.data.model.TransactionEntity
import com.khaltech.expenseassistant.data.repo.CustomCategoryOption
import com.khaltech.expenseassistant.ui.CardElevation
import com.khaltech.expenseassistant.ui.DayGroupCard
import com.khaltech.expenseassistant.ui.IncomeColor
import com.khaltech.expenseassistant.ui.SpendColor
import com.khaltech.expenseassistant.ui.formatMinor
import com.khaltech.expenseassistant.ui.pro.LocalPro
import com.khaltech.expenseassistant.ui.rememberHeroGradient
import com.khaltech.expenseassistant.ui.startOfDay

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

    // The totals above are always the real ones for the whole category. Without Pro it is the
    // itemised list that is cut short, so the number on screen is never a number we invented.
    val pro = LocalPro.current
    val visible = if (pro.isPro) transactions else transactions.take(FREE_PREVIEW_COUNT)
    val hidden = transactions.size - visible.size

    val days = visible.groupBy { startOfDay(it.occurredAt) }
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

            if (hidden > 0) {
                item {
                    SeeAllProCard(
                        total = transactions.size,
                        hidden = hidden,
                        onUpgrade = pro.onUpgrade,
                    )
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

/**
 * How much of a category's list a free user sees. Enough to recognise their own spending and judge
 * whether the full list is worth paying for; not enough to be the feature.
 */
private const val FREE_PREVIEW_COUNT = 3

/**
 * The button counts the whole category, not the part being withheld: "See all 23 transactions" is
 * what the user is being offered, where "See all 20" would be counting from a number they never saw.
 *
 * The card only appears once something is actually hidden, which needs more than [FREE_PREVIEW_COUNT]
 * transactions, so [total] is always plural here.
 */
@Composable
private fun SeeAllProCard(total: Int, hidden: Int, onUpgrade: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onUpgrade)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "  $hidden more in this category",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                "Pro shows every transaction behind a category, not just the latest few.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onUpgrade) { Text("See all $total transactions · Pro") }
        }
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
