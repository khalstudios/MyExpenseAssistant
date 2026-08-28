package com.expenseassistant.ui.insights

import com.expenseassistant.data.model.Category
import com.expenseassistant.data.model.TransactionEntity

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
) {
    val range: AnalyticsRange get() = selection.range

    val periodOverPeriodPercent: Int?
        get() = if (previousTotalSpendMinor <= 0) null
        else (((totalSpendMinor - previousTotalSpendMinor).toDouble() / previousTotalSpendMinor) * 100).toInt()

    val hasBudgets: Boolean get() = overallBudget != null || categoryBudgets.isNotEmpty()
}
