package com.khaltech.expenseassistant.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khaltech.expenseassistant.data.model.Category
import com.khaltech.expenseassistant.ui.CardElevation
import com.khaltech.expenseassistant.ui.IncomeColor
import com.khaltech.expenseassistant.ui.formatMinorWhole
import com.khaltech.expenseassistant.ui.investColor
import com.khaltech.expenseassistant.ui.rememberSoftGradient

/** Past this many rows the smallest investments are unreadable slivers; the category page lists them all. */
private const val InvestmentRows = 5

/** True when the period has anything for [SavingsCard] to say. */
val AnalyticsUiState.hasSavingsToShow: Boolean
    get() = totalInvestedMinor != 0L || previousTotalInvestedMinor != 0L || totalIncomeMinor > 0

/**
 * Money kept rather than spent: what was invested, what was saved of income besides, how investing
 * compares with the previous period, and where the investments went. Kept apart from Spending by
 * category so that card's total always matches Expenditure on Home.
 */
@Composable
fun SavingsCard(
    state: AnalyticsUiState,
    onOpenCategory: (Category) -> Unit,
    /** Opens one merchant's investments, money in and out, for this period. */
    onOpenInvestmentMerchant: (String) -> Unit,
) {
    val gold = investColor()
    val openInvestments = { onOpenCategory(Category.INVESTMENTS) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = CardElevation),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(rememberSoftGradient())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Savings, contentDescription = null, tint = gold)
                Text(
                    "  Savings & investments",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SavingsStat(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClickLabel = "See investments", onClick = openInvestments),
                    label = "INVESTED",
                    labelColor = gold,
                    amount = formatMinorWhole(state.totalInvestedMinor),
                    amountColor = gold,
                    note = investedComparison(state),
                )
                val rate = state.savingsRatePercent
                SavingsStat(
                    modifier = Modifier.weight(1f),
                    label = "SAVINGS",
                    labelColor = IncomeColor,
                    amount = formatMinorWhole(state.savingsMinor),
                    amountColor = if (state.savingsMinor > 0) IncomeColor else MaterialTheme.colorScheme.onSurface,
                    note = if (rate == null) "Add income to see this" else "$rate% of income",
                    alignEnd = true,
                )
            }

            if (state.investments.isEmpty()) {
                Text(
                    "Nothing invested in this period. SIPs, stocks and deposits show up here once " +
                        "they're in the Investments category.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                HorizontalDivider()
                Text("Where it went", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                InvestmentBars(
                    shares = state.investments.take(InvestmentRows),
                    color = gold,
                    onOpen = { share -> onOpenInvestmentMerchant(share.merchant) },
                )
                val hidden = state.investments.size - InvestmentRows
                if (hidden > 0) {
                    Text(
                        "+$hidden more · See all investments",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(onClick = openInvestments),
                    )
                }
            }
        }
    }
}

/** How this period's investing compares with the one before, or null when neither had any. */
private fun investedComparison(state: AnalyticsUiState): String? {
    val now = state.totalInvestedMinor
    val before = state.previousTotalInvestedMinor
    val previous = Periods.comparisonLabel(state.range).removePrefix("the ")
    return when {
        now == 0L && before == 0L -> null
        now == before -> "Same as $previous"
        now > before -> "${formatMinorWhole(now - before)} more than $previous"
        else -> "${formatMinorWhole(before - now)} less than $previous"
    }
}

@Composable
private fun SavingsStat(
    label: String,
    labelColor: Color,
    amount: String,
    amountColor: Color,
    note: String?,
    modifier: Modifier = Modifier,
    alignEnd: Boolean = false,
) {
    Column(
        modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = labelColor)
        Text(amount, style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp), fontWeight = FontWeight.Bold, color = amountColor)
        note?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
            )
        }
    }
}

/** One bar per merchant, measured against the largest, each naming itself so colour carries nothing alone. */
@Composable
private fun InvestmentBars(shares: List<InvestmentShare>, color: Color, onOpen: (InvestmentShare) -> Unit) {
    val max = shares.first().amountMinor.coerceAtLeast(1L)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        shares.forEach { share ->
            val fraction = (share.amountMinor.toFloat() / max).coerceIn(0.02f, 1f)
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClickLabel = "See ${share.merchant}") { onOpen(share) },
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        share.merchant,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        if (share.transactionCount > 1) "${share.transactionCount}× · " else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        formatMinorWhole(share.amountMinor),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(color.copy(alpha = 0.15f)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(fraction)
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(color),
                    )
                }
            }
        }
    }
}
