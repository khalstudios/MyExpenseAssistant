package com.khaltech.expenseassistant.ui

import com.khaltech.expenseassistant.ui.insights.BudgetProgress

/** How the current spending reads: on track, worth watching, or past the limit. */
enum class SpendingMood { GOOD, WATCH, OVER }

data class SpendingStatus(val mood: SpendingMood, val headline: String, val detail: String)

/**
 * Turns today's and this month's numbers into one plain-language line for the home banner.
 *
 * Returns null when no overall monthly budget is set: without a limit there is nothing to say about
 * doing well or badly, and a prompt on every launch would only nag.
 *
 * [dailyShareMinor] is the monthly budget spread evenly across the month; [daysLeft] counts the days
 * after today.
 */
fun spendingStatus(
    todaySpendMinor: Long,
    dailyShareMinor: Long,
    budget: BudgetProgress?,
    daysLeft: Int,
): SpendingStatus? {
    if (budget == null || budget.limitMinor <= 0) return null

    val daysClause = when {
        daysLeft <= 0 -> "on the last day of the month"
        daysLeft == 1 -> "with 1 day to go"
        else -> "with $daysLeft days to go"
    }

    return when {
        budget.isOverBudget -> SpendingStatus(
            mood = SpendingMood.OVER,
            headline = "Over budget",
            detail = "${formatMinor(-budget.remainingMinor)} past this month's budget, $daysClause.",
        )

        budget.isAheadOfPace -> SpendingStatus(
            mood = SpendingMood.WATCH,
            headline = "Spending fast",
            detail = "${formatMinor(budget.remainingMinor)} left $daysClause — ahead of an even pace.",
        )

        dailyShareMinor > 0 && todaySpendMinor > dailyShareMinor -> SpendingStatus(
            mood = SpendingMood.WATCH,
            headline = "Heavy day",
            detail = "${formatMinor(todaySpendMinor - dailyShareMinor)} over today's share, " +
                "but the month is still on track.",
        )

        todaySpendMinor == 0L -> SpendingStatus(
            mood = SpendingMood.GOOD,
            headline = "Nothing spent yet today",
            detail = "${formatMinor(budget.remainingMinor)} left for the month, $daysClause.",
        )

        else -> SpendingStatus(
            mood = SpendingMood.GOOD,
            headline = "Looking good",
            detail = "Within budget today and for the month — " +
                "${formatMinor(budget.remainingMinor)} left $daysClause.",
        )
    }
}
