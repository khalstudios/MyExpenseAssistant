package com.khaltech.expenseassistant.ui.insights

import com.khaltech.expenseassistant.data.model.Category
import com.khaltech.expenseassistant.data.model.TransactionEntity
import com.khaltech.expenseassistant.data.repo.TagUsage

data class BudgetProgress(
    val category: Category?,
    val limitMinor: Long,
    val spentMinor: Long,
    /** How far through the period we are, used to judge pace. */
    val paceFraction: Float,
) {
    val fraction: Float get() = if (limitMinor > 0) spentMinor.toFloat() / limitMinor else 0f
    val remainingMinor: Long get() = limitMinor - spentMinor
    val isOverBudget: Boolean get() = spentMinor > limitMinor
    val isAheadOfPace: Boolean get() = !isOverBudget && fraction > paceFraction
}

/** Money invested with one merchant over the period, for the savings card's "where it went" rows. */
data class InvestmentShare(
    val merchant: String,
    val amountMinor: Long,
    val transactionCount: Int,
)

data class AnalyticsUiState(
    val selection: PeriodSelection = PeriodSelection.now(AnalyticsRange.MONTH),
    val transactions: List<TransactionEntity> = emptyList(),
    /** The [Periods.ComparablePeriods] periods before [selection], for the momentum card's "compare with" lines. */
    val previousTransactions: List<TransactionEntity> = emptyList(),
    val periodLabel: String = "",
    val canGoForward: Boolean = false,
    val isCurrentPeriod: Boolean = true,
    val slices: List<PieSlice> = emptyList(),
    val totalSpendMinor: Long = 0,
    val totalIncomeMinor: Long = 0,
    val previousTotalSpendMinor: Long = 0,
    /** Investments are saved, not spent, so they are kept apart from every spending figure. */
    val totalInvestedMinor: Long = 0,
    val previousTotalInvestedMinor: Long = 0,
    val investments: List<InvestmentShare> = emptyList(),
    val dailyAverageMinor: Long = 0,
    val projectedTotalMinor: Long = 0,
    val transactionCount: Int = 0,
    val activeDays: Int = 0,
    val largestTransaction: TransactionEntity? = null,
    val topMerchant: Pair<String, Long>? = null,
    /** Where tapping the top merchant leads: its Merchants page, or its largest payment when it has no page. */
    val topMerchantKey: String? = null,
    val topMerchantLargestId: Long? = null,
    val needsReviewCount: Int = 0,
    val overallBudget: BudgetProgress? = null,
    val categoryBudgets: List<BudgetProgress> = emptyList(),
    /** Tags used inside this period only, so the card tracks the selected week/month/year. */
    val tagUsage: List<TagUsage> = emptyList(),
) {
    val range: AnalyticsRange get() = selection.range

    val periodOverPeriodPercent: Int?
        get() = if (previousTotalSpendMinor <= 0) null
        else (((totalSpendMinor - previousTotalSpendMinor).toDouble() / previousTotalSpendMinor) * 100).toInt()

    val hasBudgets: Boolean get() = overallBudget != null || categoryBudgets.isNotEmpty()

    /** Income left after spending and investing, never below zero; investments are shown on their own. */
    val savingsMinor: Long
        get() = (totalIncomeMinor - totalSpendMinor - totalInvestedMinor).coerceAtLeast(0)

    /** Share of income saved, or null with no income to measure it against. */
    val savingsRatePercent: Int?
        get() = if (totalIncomeMinor <= 0) null else (savingsMinor * 100 / totalIncomeMinor).toInt()
}
