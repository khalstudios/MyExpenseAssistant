package com.khaltech.expenseassistant.ui

import com.khaltech.expenseassistant.data.model.Category

/**
 * Short one-liners about today, for the bar above the bottom navigation. Every applicable line is
 * returned so the caller can pick one at random and the message differs between openings.
 *
 * [todayByCategory] is today's spending per category, largest first. [dailyBudgetMinor] is the cap
 * today is measured against, or zero when none is set.
 */
fun spendingTips(
    todaySpendMinor: Long,
    dailyBudgetMinor: Long,
    todayByCategory: List<Pair<Category, Long>>,
    todayCount: Int,
): List<String> {
    val tips = mutableListOf<String>()
    val top = todayByCategory.firstOrNull()
    val remaining = dailyBudgetMinor - todaySpendMinor

    if (todaySpendMinor == 0L) {
        tips += "Nothing spent yet today — a clean slate."
        if (dailyBudgetMinor > 0) {
            tips += "${formatMinor(dailyBudgetMinor)} to play with today."
        }
        return tips
    }

    // What today looks like on its own, with no budget to judge it against.
    tips += "${formatMinor(todaySpendMinor)} spent today across " +
        "$todayCount ${if (todayCount == 1) "spend" else "spends"}."
    top?.let { (category, amount) ->
        tips += "${formatMinor(amount)} on ${category.displayName} today, your biggest category."
    }

    if (dailyBudgetMinor <= 0) {
        tips += "Set a daily budget and I can tell you how today is tracking."
        return tips
    }

    when {
        remaining < 0 -> {
            tips += "You're ${formatMinor(-remaining)} over today's budget."
            top?.let { (category, _) ->
                tips += "Today went over budget — ${category.displayName} led the way."
            }
        }

        // Close enough that the next ordinary spend would tip it over.
        remaining <= dailyBudgetMinor / 5 -> {
            tips += "Only ${formatMinor(remaining)} left of today's budget."
            top?.let { (category, _) ->
                tips += "One more ${category.displayName} spend would cross today's budget."
            }
        }

        else -> {
            tips += "${formatMinor(remaining)} still left of today's budget — doing well."
            tips += "You're inside today's budget with ${formatMinor(remaining)} to spare."
        }
    }

    return tips
}
