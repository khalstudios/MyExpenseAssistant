package com.khaltech.expenseassistant.ui

/**
 * One-off hints that stand in for the walkthrough on a new install: each is shown once, the first
 * time its feature has something on screen to explain.
 */
enum class FeatureHint(val key: String, val text: String) {
    EDIT_TRANSACTION(
        "hint_edit_transaction",
        "Long-press a payment to change its category. Tap it to edit the details or add tags.",
    ),
    INSIGHTS(
        "hint_insights",
        "Insights looks at longer stretches: month-by-month trends, recurring payments and tagged spending.",
    ),
    BUDGET(
        "hint_budget",
        "Set a monthly limit overall or per category, and Home shows how much of it you've used.",
    ),
}
