package com.khaltech.expenseassistant.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.sp
import com.khaltech.expenseassistant.data.model.Category
import com.khaltech.expenseassistant.data.model.Direction
import com.khaltech.expenseassistant.data.model.TransactionEntity
import com.khaltech.expenseassistant.data.repo.CustomCategoryOption
import com.khaltech.expenseassistant.ui.budget.BudgetAmountDialog
import com.khaltech.expenseassistant.ui.budget.BudgetOverviewCard
import com.khaltech.expenseassistant.ui.category.CategoryBadge
import com.khaltech.expenseassistant.ui.category.CategoryDot
import com.khaltech.expenseassistant.ui.category.CategoryPickerSheet
import com.khaltech.expenseassistant.ui.category.displayCategoryName
import com.khaltech.expenseassistant.ui.insights.AnalyticsUiState
import com.khaltech.expenseassistant.ui.insights.CategoryPieChart
import com.khaltech.expenseassistant.ui.insights.PieSlice
import kotlinx.coroutines.launch

private const val MillisPerDay = 24L * 60 * 60 * 1000

/** Below titleLarge, so a seven-figure income and expenditure still sit side by side. */
private val SummaryAmountSize = 20.sp

/** Below titleLarge, so a four-figure day of spending is not crowded by the pace line beside it. */
private val TodayAmountSize = 19.sp

@OptIn(ExperimentalFoundationApi::class)

@Composable
fun HomeScreen(
    state: HomeUiState,
    notificationAccessGranted: Boolean,
    showBackupNotice: Boolean = false,
    /** True when automatic backups are locked behind Pro, which turns the notice into an upgrade offer. */
    backupNeedsPro: Boolean = false,
    onEnableBackup: () -> Unit = {},
    onDismissBackupNotice: () -> Unit = {},
    onCategoryChange: (Long, Category) -> Unit,
    onCategoryChangeCustom: (Long, String, String, String) -> Unit = { _, _, _, _ -> },
    customCategories: List<CustomCategoryOption> = emptyList(),
    summaryScope: SummaryScope = SummaryScope.MONTH,
    onSummaryScopeChange: (SummaryScope) -> Unit = {},
    budgetState: AnalyticsUiState? = null,
    onOpenCategory: (Category) -> Unit = {},
    onDelete: (Long) -> Unit,
    onOpenTransaction: (Long) -> Unit,
    dailyBudgetMinor: Long = 0,
    dailyBudgetIsExplicit: Boolean = false,
    onSetDailyBudget: (Long) -> Unit = {},
    spendingStatus: SpendingStatus? = null,
    categoryFilter: Category? = null,
    tagFilter: String? = null,
    needsReviewFilter: Boolean = false,
    onOpenNeedsReview: () -> Unit = {},
    onOpenAllTransactions: () -> Unit = {},
    onOpenSummaryTransactions: (Direction) -> Unit = {},
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

    // The home feed only shows today plus the two previous days; everything else lives behind "See more".
    val recentOnly = !hasFilter && transactionOverride == null
    val recentCutoff = startOfDay(System.currentTimeMillis() - 2 * MillisPerDay)
    val shownDays = if (recentOnly) days.filter { it.first >= recentCutoff } else days

    val today = dayRange(System.currentTimeMillis())
    // Biggest first: the card only has room for a few rows, and the ones worth that room are the
    // ones that moved the day's total, not the ones that happened to be most recent.
    val todaySpending = state.transactions
        .filter { it.occurredAt in today && it.direction == Direction.DEBIT }
        .sortedWith(bySizeThenRecency)

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // Items emitted ahead of the "Latest activity" header, so the today card can scroll straight to
    // it. Kept in step with the item order below.
    val latestActivityIndex = 1 + // permissions card, emitted even when it draws nothing
        (if (showBackupNotice) 1 else 0) +
        1 + // today card
        (if (spendingStatus != null) 1 else 0) +
        2 + // "Income & Expenditure" header and summary card
        (if (state.spendByCategory.isNotEmpty()) 2 else 0) +
        (if (budgetState != null) 2 else 0)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (!hasFilter && transactionOverride == null) {
            item {
                PermissionsCard(notificationAccessGranted)
            }
            if (showBackupNotice) {
                item {
                    BackupNoticeCard(
                        needsPro = backupNeedsPro,
                        onEnable = onEnableBackup,
                        onDismiss = onDismissBackupNotice,
                    )
                }
            }
            item {
                TodaySpendCard(
                    todaySpendMinor = state.todaySpendMinor,
                    todaySpendCount = state.todaySpendCount,
                    dailyBudgetMinor = dailyBudgetMinor,
                    dailyBudgetIsExplicit = dailyBudgetIsExplicit,
                    onSetDailyBudget = onSetDailyBudget,
                    todaySpending = todaySpending,
                    onJumpToLatest = {
                        scope.launch { listState.animateScrollToItem(latestActivityIndex) }
                    },
                )
            }
            spendingStatus?.let { status ->
                item { SpendingStatusCard(status) }
            }
            item { SectionHeader("Income & Expenditure", topPadding = 0.dp) }
            item { SummaryCard(state, summaryScope, onSummaryScopeChange, onOpenNeedsReview, onOpenSummaryTransactions) }

            if (state.spendByCategory.isNotEmpty()) {
                item { SectionHeader("Where it went") }
                item { CategoryBreakdown(state.spendByCategory, onOpenCategory) }
            }

            budgetState?.let { monthlyBudget ->
                item { SectionHeader("Budget overview") }
                item { BudgetOverviewCard(monthlyBudget.overallBudget, monthlyBudget.categoryBudgets) }
            }
        }

        item {
            SectionHeader(if (filterLabel == null) "Latest activity" else "$filterLabel activity")
        }

        if (shownDays.isEmpty()) {
            item {
                Text(
                    when {
                        filterLabel != null -> "No transactions found."
                        recentOnly -> "No transactions in the last 3 days."
                        else -> "No transactions this month."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(shownDays, key = { it.first }) { (dayStart, dayTransactions) ->
            DayGroupCard(
                dayStart = dayStart,
                transactions = dayTransactions,
                onOpenTransaction = onOpenTransaction,
                onEditCategory = { editing = it },
                onDelete = onDelete,
            )
        }

        if (recentOnly) {
            item {
                TextButton(onClick = onOpenAllTransactions, modifier = Modifier.fillMaxWidth()) {
                    Text("See more")
                }
            }
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

/**
 * At-a-glance banner for the money spent since midnight, above the period summary. It turns amber
 * once the day has outspent its even share of the overall monthly budget, and expands in place to
 * list the day's spending.
 */
@Composable
private fun TodaySpendCard(
    todaySpendMinor: Long,
    todaySpendCount: Int,
    dailyBudgetMinor: Long,
    dailyBudgetIsExplicit: Boolean,
    onSetDailyBudget: (Long) -> Unit,
    todaySpending: List<TransactionEntity>,
    onJumpToLatest: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var editingDailyBudget by remember { mutableStateOf(false) }
    val spentSomething = todaySpendMinor > 0
    val overDailyPace = dailyBudgetMinor > 0 && todaySpendMinor > dailyBudgetMinor
    // Half the day's cap is where the card turns from green to red: a warning that today is
    // running hot while there is still budget left to protect.
    val pastHalfDailyBudget = dailyBudgetMinor > 0 && todaySpendMinor * 2 > dailyBudgetMinor
    val accent = when {
        // With no cap there is no halfway mark to measure against, so any spending reads as spend.
        dailyBudgetMinor > 0 -> if (pastHalfDailyBudget) SpendColor else IncomeColor
        spentSomething -> SpendColor
        else -> IncomeColor
    }
    val countText = "$todaySpendCount ${if (todaySpendCount == 1) "transaction" else "transactions"}"
    val paceText = when {
        overDailyPace -> "${formatMinor(todaySpendMinor - dailyBudgetMinor)} over today's pace"
        else -> "${formatMinor(dailyBudgetMinor - todaySpendMinor)} left of today's pace"
    }
    // Blended to an opaque fill: a translucent container lets the card's own shadow through, which
    // reads as a muddy frame in light mode.
    val container = accent.copy(alpha = 0.12f).compositeOver(MaterialTheme.colorScheme.surface)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = CardElevation),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .then(
                        // Nothing to expand into on a day with no spending.
                        if (spentSomething) Modifier.clickable { expanded = !expanded } else Modifier
                    )
                    // The chevron carries its own optical padding inside a 24dp box, so the end
                    // inset is trimmed when it is there; without it the amount needs the full inset.
                    .padding(
                        start = 14.dp,
                        end = if (spentSomething) 8.dp else 14.dp,
                        top = 14.dp,
                        bottom = 14.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accent.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (overDailyPace) Icons.Filled.WarningAmber else Icons.Filled.Today,
                        contentDescription = null,
                        tint = accent,
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "SPENT TODAY →",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        if (spentSomething) countText else "Nothing spent yet today",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    // Deliberately allowed to truncate: the expanded card carries the full figure.
                    if (spentSomething && dailyBudgetMinor > 0) {
                        Text(
                            paceText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                // Centred rather than right-aligned: the amount and the budget pill below it are
                // different widths, and centring keeps the pair reading as one stacked unit.
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        formatMinor(todaySpendMinor),
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = TodayAmountSize,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                        maxLines = 1,
                    )
                    // The day's cap sits under the figure it caps, and a tap edits it without
                    // opening the card — including on a day with nothing spent to expand into.
                    val budgetPillInteraction = remember { MutableInteractionSource() }
                    Row(
                        Modifier
                            .padding(top = 2.dp)
                            // The pill is drawn small, but takes taps from around it too; the ripple
                            // stays on the pill itself.
                            .expandTouchArea(horizontal = 10.dp, vertical = 12.dp)
                            .clickable(
                                interactionSource = budgetPillInteraction,
                                indication = null,
                                onClickLabel = "Edit daily budget",
                            ) { editingDailyBudget = true }
                            .padding(horizontal = 10.dp, vertical = 12.dp)
                            .clip(RoundedCornerShape(50))
                            .background(accent.copy(alpha = 0.16f))
                            .indication(budgetPillInteraction, ripple())
                            .padding(start = 8.dp, end = 6.dp, top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            if (dailyBudgetMinor > 0) "of ${formatMinor(dailyBudgetMinor)}" else "Set budget",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
                if (spentSomething) {
                    Icon(
                        Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "Hide today's spending" else "Show today's spending",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.rotate(if (expanded) 180f else 0f),
                    )
                }
            }

            if (expanded && spentSomething) {
                HorizontalDivider(color = accent.copy(alpha = 0.22f))
                Column(
                    Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // The pace line lives here rather than in the header, which keeps the collapsed
                    // card one row tall no matter how long the figure runs.
                    if (dailyBudgetMinor > 0) {
                        Text(
                            paceText,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = accent,
                        )
                    }
                    todaySpending.take(TodayRowLimit).forEach { transaction ->
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            CategoryDot(transaction.category)
                            Text(
                                transaction.displayTitle,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                formatTimeOnly(transaction.occurredAt),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                formatMinor(transaction.amountMinor),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = accent,
                            )
                        }
                    }
                    if (todaySpending.size > TodayRowLimit) {
                        Text(
                            "+${todaySpending.size - TodayRowLimit} more today",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    // Also editable from the pill in the header; here it sits with the full figure.
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { editingDailyBudget = true }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            "Daily budget",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            when {
                                dailyBudgetMinor <= 0 -> "Set"
                                dailyBudgetIsExplicit -> formatMinor(dailyBudgetMinor)
                                else -> "${formatMinor(dailyBudgetMinor)} (from monthly)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Edit daily budget",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onJumpToLatest)
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            "Go to Latest activity",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = accent,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            Icons.Filled.ArrowForward,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }

    if (editingDailyBudget) {
        BudgetAmountDialog(
            title = "Budget for today",
            initialMinor = dailyBudgetMinor,
            fieldLabel = "Daily limit",
            hint = if (dailyBudgetMinor > 0 && !dailyBudgetIsExplicit) "Worked out from your monthly budget" else null,
            onDismiss = { editingDailyBudget = false },
            onConfirm = {
                onSetDailyBudget(it)
                editingDailyBudget = false
            },
        )
    }
}

/** Longer days collapse to a count rather than turning the banner into a second feed. */
private const val TodayRowLimit = 5

/**
 * Takes up the size of the content after the following padding of [horizontal] and [vertical], so
 * a click modifier between the two catches taps in the padding while the layout around it sees
 * only the content. Nothing may clip the spilled area.
 */
private fun Modifier.expandTouchArea(horizontal: Dp, vertical: Dp): Modifier = layout { measurable, constraints ->
    val dx = horizontal.roundToPx()
    val dy = vertical.roundToPx()
    val placeable = measurable.measure(constraints.offset(2 * dx, 2 * dy))
    layout((placeable.width - 2 * dx).coerceAtLeast(0), (placeable.height - 2 * dy).coerceAtLeast(0)) {
        placeable.place(-dx, -dy)
    }
}

@Composable
private fun SectionHeader(title: String, topPadding: androidx.compose.ui.unit.Dp = 4.dp) {
    Text(
        title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = topPadding),
    )
}

/**
 * Today's spending, largest amount first, with the most recent breaking a tie: the Today card has
 * room for only a few rows, so it leads with the ones that moved the day's total.
 */
private val bySizeThenRecency: Comparator<TransactionEntity> =
    compareByDescending<TransactionEntity> { it.amountMinor }.thenByDescending { it.occurredAt }

/**
 * One card per day, matching how the reference app groups a day's spending together. Newest first
 * within the day, like the days themselves, so the payment just made is the one at the top.
 */
@Composable
fun DayGroupCard(
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
    onOpenTransactions: (Direction) -> Unit,
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
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    scope.headline,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = hero.onGradient,
                )
                Text(
                    when (scope) {
                        SummaryScope.MONTH -> currentMonthYearName()
                        SummaryScope.YEAR -> yearToDateLabel()
                        SummaryScope.ALL -> scope.label
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = hero.onGradientMuted,
                )
            }
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
            // Each side takes half the row, so a seven-figure amount ellipsizes instead of running
            // into the other column.
            // Each amount opens the transactions behind it, over the scope the card is showing.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClickLabel = "See income") { onOpenTransactions(Direction.CREDIT) },
                ) {
                    Text(
                        "INCOME",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = IncomeColor,
                    )
                    Text(
                        formatMinorWhole(state.incomeMinor),
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = SummaryAmountSize,
                        fontWeight = FontWeight.Bold,
                        color = hero.onGradient,
                        maxLines = 1,
                    )
                }
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClickLabel = "See expenses") { onOpenTransactions(Direction.DEBIT) },
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        "EXPENDITURE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = SpendColor,
                    )
                    Text(
                        formatMinorWhole(state.spendMinor),
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = SummaryAmountSize,
                        fontWeight = FontWeight.Bold,
                        color = hero.onGradient,
                        maxLines = 1,
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
private fun CategoryBreakdown(
    breakdown: List<Pair<Category, Long>>,
    onOpenCategory: (Category) -> Unit,
) {
    val totalMinor = breakdown.sumOf { it.second }
    val slices = breakdown.map { (category, amountMinor) ->
        PieSlice(
            category = category,
            amountMinor = amountMinor,
            fraction = amountMinor.toFloat() / totalMinor.coerceAtLeast(1L),
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = CardElevation),
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                CategoryPieChart(
                    slices = slices,
                    modifier = Modifier.widthIn(max = 260.dp),
                )
            }
            breakdown.take(6).forEach { (category, amount) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenCategory(category) }
                        .padding(vertical = 4.dp, horizontal = 4.dp),
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
                transaction.displayTitle,
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
