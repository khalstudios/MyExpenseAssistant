package com.expenseassistant.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expenseassistant.recurring.RecurringExpense
import com.expenseassistant.ui.category.CategoryBadge
import com.expenseassistant.ui.formatMinor
import com.expenseassistant.ui.rememberSoftGradient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val nextDueFormat = SimpleDateFormat("d MMM", Locale.getDefault())

@Composable
fun RecurringCard(items: List<RecurringExpense>, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(rememberSoftGradient())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Autorenew, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    "  Recurring & subscriptions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                "Detected from repeat payments over the last six months.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider()
            items.forEach { item ->
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CategoryBadge(item.category, size = 38.dp)
                    Column(Modifier.weight(1f)) {
                        Text(item.merchant, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "${item.cadence.label} \u00b7 next around ${nextDueFormat.format(Date(item.nextExpectedAt))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        formatMinor(item.typicalAmountMinor),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
