package com.khaltech.expenseassistant.data.model

import androidx.room.Entity

/**
 * A spending limit for one [period]. [categoryKey] is a [Category] name, or [OVERALL] for all
 * spending taken together.
 */
@Entity(tableName = "budgets", primaryKeys = ["categoryKey", "period"])
data class BudgetEntity(
    val categoryKey: String,
    val period: BudgetPeriod,
    val limitMinor: Long,
    val updatedAt: Long = System.currentTimeMillis(),
) {
    companion object {
        const val OVERALL = "__OVERALL__"

        /** The key a pre-period database used for the day's cap, folded into OVERALL/DAILY. */
        const val LEGACY_DAILY = "__DAILY__"

        fun keyFor(category: Category?): String = category?.name ?: OVERALL
    }
}
