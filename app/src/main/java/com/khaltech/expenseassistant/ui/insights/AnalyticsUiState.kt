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

data class AnalyticsUiState(
    val selection: PeriodSelection = PeriodSelection.now(AnalyticsRange.MONTH),
    val transactions: List<TransactionEntity> = emptyList(),
    /** The period before [selection], for the momentum card's "compare with" overlay. */
    val previousTransactions: List<TransactionEntity> = emptyList(),
    val periodLabel: String = "",
    val canGoForward: Boolean = false,
    val isCurrentPeriod: Boolean = true,
    val slices: List<PieSlice> = emptyList(),
    val totalSpendMinor: Long = 0,
    val totalIncomeMinor: Long = 0,
    val previousTotalSpendMinor: Long = 0,
    val dailyAverageMinor: Long = 0,
    val projectedTotalMinor: Long = 0,
    val transactionCount: Int = 0,
    val activeDays: Int = 0,
    val largestTransaction: TransactionEntity? = null,
    val topMerchant: Pair<String, Long>? = null,
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
}
