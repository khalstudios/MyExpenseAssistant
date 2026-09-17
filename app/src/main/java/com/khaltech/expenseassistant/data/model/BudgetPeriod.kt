package com.khaltech.expenseassistant.data.model

/** The stretch of time a budget's limit covers. */
enum class BudgetPeriod(val displayName: String) {
    DAILY("Daily"),
    MONTHLY("Monthly"),
    YEARLY("Yearly");

    companion object {
        /** Budgets stored before periods existed were monthly, so that is the fallback. */
        fun fromName(value: String?): BudgetPeriod =
            entries.firstOrNull { it.name == value } ?: MONTHLY
    }
}
