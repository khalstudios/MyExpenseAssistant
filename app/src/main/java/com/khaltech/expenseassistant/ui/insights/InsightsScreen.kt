package com.khaltech.expenseassistant.ui.insights

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.khaltech.expenseassistant.ui.category.CategoryBadge
import com.khaltech.expenseassistant.ui.CardElevation
import com.khaltech.expenseassistant.ui.formatMinor
import com.khaltech.expenseassistant.ui.rememberSoftGradient
import com.khaltech.expenseassistant.data.model.Category
import com.khaltech.expenseassistant.data.repo.TagUsage
import com.khaltech.expenseassistant.ui.pro.FreeTagLimit
import com.khaltech.expenseassistant.ui.pro.LocalPro
import com.khaltech.expenseassistant.ui.pro.ProLocked
import com.khaltech.expenseassistant.recurring.RecurringExpense
import kotlin.math.abs

@Composable
fun InsightsScreen(
    state: AnalyticsUiState,
    recurring: List<RecurringExpense>,
    onRangeChange: (AnalyticsRange) -> Unit,
    onShiftPeriod: (Int) -> Unit,
    onJumpTo: (year: Int, monthIndex: Int) -> Unit,
    onResetToCurrent: () -> Unit,
    onOpenCategory: (Category) -> Unit,
    onOpenTag: (String) -> Unit,
    onOpenNeedsReview: () -> Unit = {},
    onAddRecurring: () -> Unit = {},
    onOpenRecurring: (RecurringExpense) -> Unit = {},
    onOpenMerchant: (String) -> Unit = {},
    onOpenTransaction: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // Read here rather than inside the list: a LazyColumn's content block is not a composable scope.
    val isPro = LocalPro.current.isPro
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
        // The period's headline numbers come first: how much a day costs, where the period is
        // heading, and how that compares to the last one. The category breakdown answers "on what",
        // which is only worth reading once "how much" has been answered.
        item { StatGrid(state) }
        item { ComparisonCard(state) }
        item { SpendByCategoryCard(state, onOpenCategory) }
        if (state.tagUsage.isNotEmpty()) {
            item { TagsCard(state.tagUsage, onOpenTag) }
        }
        item { SpendingTrendsCard(state) }
        if (recurring.isNotEmpty() || isPro) {
            item {
                ProLocked(
                    title = "Recurring payments",
                    subtitle = "Find the subscriptions and standing charges you have stopped noticing.",
                ) {
                    RecurringCard(recurring, onAdd = onAddRecurring, onOpen = onOpenRecurring)
                }
            }
        }
        state.topMerchant?.let { (merchant, amount) ->
            val key = state.topMerchantKey
            val largestId = state.topMerchantLargestId
            val onClick = when {
                key != null -> { -> onOpenMerchant(key) }
                largestId != null -> { -> onOpenTransaction(largestId) }
                else -> null
            }
            item { TopMerchantCard(merchant, amount, onClick) }
        }
        state.largestTransaction?.let { transaction ->
            item {
                HighlightCard(
                    icon = Icons.Filled.NorthEast,
                    title = "Largest single expense",
                    primary = formatMinor(transaction.amountMinor),
                    secondary = transaction.merchant,
                    badge = { CategoryBadge(transaction, size = 40.dp) },
                    onClick = { onOpenTransaction(transaction.id) },
                )
            }
        }
        if (state.needsReviewCount > 0) {
            item { ReviewNudgeCard(state.needsReviewCount, onOpenNeedsReview) }
        }
    }
}

/**
 * What each tag costs over the period, free for everyone.
 *
 * This card is the argument for tagging, so hiding it behind the paywall hid the very thing that
 * would make someone want to pay: a user who has never seen their tags priced has nothing to miss.
 * What Pro sells here is the two things a user reaches for once this card has done its work:
 * a sixth tag, and every transaction behind a tag rather than the first few.
 *
 * A free user's list is capped at [FreeTagLimit], the same number of tags they can create, so the
 * cap only ever truncates for someone who kept more tags from a lapsed or grandfathered Pro. Both
 * halves of the card take the same cut: the chart and the chips must not disagree about which tags
 * exist.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsCard(tags: List<TagUsage>, onOpenTag: (String) -> Unit) {
    val pro = LocalPro.current
    val visible = if (pro.isPro) tags else tags.take(FreeTagLimit)
    val hidden = tags.size - visible.size

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
                "Tap a tag to see everything tagged with it in this period.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val spendingTags = visible.filter { it.spentMinor > 0 }.sortedByDescending { it.spentMinor }
            if (spendingTags.isNotEmpty()) {
                TagSpendBarChart(spendingTags, onOpenTag)
                HorizontalDivider()
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                visible.forEach { usage ->
                    AssistChip(
                        onClick = { onOpenTag(usage.tag) },
                        label = { Text("#${usage.tag} \u00b7 ${usage.count}") },
                    )
                }
            }
            if (!pro.isPro) {
                HorizontalDivider()
                TagsProFooter(total = tags.size, hidden = hidden, onUpgrade = pro.onUpgrade)
            }
        }
    }
}

/**
 * The offer under a free user's tag card.
 *
 * Two different things are being withheld and only one of them is usually in play, so the copy says
 * whichever is true rather than listing both. Claiming tags are hidden when none are would be the
 * fastest way to teach someone that the prompts here are not worth reading.
 */
@Composable
private fun TagsProFooter(total: Int, hidden: Int, onUpgrade: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Text(
                if (hidden > 0) "  $hidden more ${if (hidden == 1) "tag" else "tags"}" else "  More tags",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            if (hidden > 0) {
                "Pro charts all $total of your tags, and shows every transaction behind each one."
            } else {
                "Free covers $FreeTagLimit tags of your own. Pro lifts that, and shows every " +
                    "transaction behind a tag rather than the first few."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onUpgrade, contentPadding = PaddingValues(0.dp)) { Text("Unlock Pro") }
    }
}

/**
 * How many bars the chart draws, whatever the user is entitled to.
 *
 * This is a drawing limit, not an entitlement: past six bars the card stops being a glance and the
 * smallest ones are unreadable anyway. It happens to sit next to [FreeTagLimit], which is five and
 * means something entirely different — a free user's list is already cut to five before it reaches
 * here, so this only ever bites for Pro, whose tag list is uncapped. Anything withheld for money is
 * decided in [TagsCard]; nothing here.
 */
private const val TagChartBars = 6

@Composable
private fun TagSpendBarChart(tags: List<TagUsage>, onOpenTag: (String) -> Unit) {
    val top = tags.take(TagChartBars)
    val max = top.first().spentMinor.coerceAtLeast(1L)
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        top.forEach { usage ->
            val fraction = (usage.spentMinor.toFloat() / max).coerceIn(0.02f, 1f)
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable { onOpenTag(usage.tag) },
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("#${usage.tag}", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        formatMinor(usage.spentMinor),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(fraction)
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
            }
        }
    }
}

/**
 * Where the period's money went: the ranked bars, with the full list folded away underneath.
 *
 * One encoding, not three. The bars already answer the share question — they are drawn against an
 * axis labelled to 100%, under the total — so the donut that used to sit below them restated the
 * same numbers in the form people read least reliably, at the cost of a full-width square of
 * scroll. Length against a common baseline beats comparing angles, and it keeps working as the
 * category list grows, where a nineteen-slice ring becomes unreadable slivers.
 *
 * Each bar carries its own name and badge, so the chart never asks the reader to identify a
 * category by its colour alone.
 */
@Composable
private fun SpendByCategoryCard(state: AnalyticsUiState, onOpenCategory: (Category) -> Unit) {
    // Saveable, not remembered: this is a lazy list item, and the card is disposed the moment it
    // scrolls off. A plain remember would silently collapse it behind the user's back.
    var detailsOpen by rememberSaveable { mutableStateOf(false) }

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
                CategoryRankedBarChart(
                    slices = state.slices,
                    totalMinor = state.totalSpendMinor,
                    onOpenCategory = onOpenCategory,
                )
                // Always offered: beyond any categories the bars leave out, it gives each one's exact
                // amount and transaction count, which the bars' percentages do not.
                HorizontalDivider()
                DetailedBreakdown(
                    slices = state.slices,
                    isOpen = detailsOpen,
                    onToggle = { detailsOpen = !detailsOpen },
                    onOpenCategory = onOpenCategory,
                )
            }
        }
    }
}

/** The long tail, behind one tap: every category with its exact amount and share. */
@Composable
private fun DetailedBreakdown(
    slices: List<PieSlice>,
    isOpen: Boolean,
    onToggle: () -> Unit,
    onOpenCategory: (Category) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .semantics { role = Role.Button },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Detailed breakdown",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${slices.size} categories",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    if (isOpen) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (isOpen) "Hide detailed breakdown" else "Show detailed breakdown",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        AnimatedVisibility(visible = isOpen) {
            CategorySpendList(slices, onOpenCategory)
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
private fun TopMerchantCard(merchant: String, amountMinor: Long, onClick: (() -> Unit)?) {
    HighlightCard(
        icon = Icons.Filled.Storefront,
        title = "Most spent at",
        primary = merchant,
        secondary = formatMinor(amountMinor),
        onClick = onClick,
    )
}

@Composable
private fun HighlightCard(
    icon: ImageVector,
    title: String,
    primary: String,
    secondary: String,
    badge: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Card(Modifier.fillMaxWidth().then(clickModifier)) {
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
