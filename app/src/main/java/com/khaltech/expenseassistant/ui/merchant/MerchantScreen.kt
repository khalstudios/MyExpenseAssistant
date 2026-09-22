package com.khaltech.expenseassistant.ui.merchant

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.khaltech.expenseassistant.data.model.TransactionEntity
import com.khaltech.expenseassistant.data.repo.MerchantSummary
import com.khaltech.expenseassistant.ui.CardElevation
import com.khaltech.expenseassistant.ui.category.CategoryBadge
import com.khaltech.expenseassistant.ui.category.displayCategoryName
import com.khaltech.expenseassistant.ui.formatMinor
import com.khaltech.expenseassistant.ui.formatTimestamp
import com.khaltech.expenseassistant.ui.rememberHeroGradient
import com.khaltech.expenseassistant.ui.rememberSoftGradient

/** Every payment made to one merchant, newest first. Read-only: each opens its own details. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantScreen(
    merchant: MerchantSummary,
    payments: List<TransactionEntity>,
    onBack: () -> Unit,
    onOpenTransaction: (Long) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(merchant.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
            item { MerchantTotalsCard(merchant) }
            item {
                Text("Payments", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            items(payments, key = { it.id }) { payment ->
                PaymentRow(payment, onClick = { onOpenTransaction(payment.id) })
            }
        }
    }
}

@Composable
private fun MerchantTotalsCard(merchant: MerchantSummary) {
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
                "${merchant.paymentCount} ${if (merchant.paymentCount == 1) "payment" else "payments"}",
                style = MaterialTheme.typography.labelMedium,
                color = hero.onGradientMuted,
            )
            Text("Spent", style = MaterialTheme.typography.labelMedium, color = hero.onGradientMuted)
            Text(
                formatMinor(merchant.spentMinor),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = hero.onGradient,
            )
            Text(
                "Last paid ${formatTimestamp(merchant.lastPaidAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = hero.onGradientMuted,
            )
        }
    }
}

@Composable
private fun PaymentRow(payment: TransactionEntity, onClick: () -> Unit) {
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
            CategoryBadge(payment, size = 42.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    payment.displayTitle,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${payment.displayCategoryName} · ${formatTimestamp(payment.occurredAt)}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                "- " + formatMinor(payment.amountMinor),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
