package com.expenseassistant.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Monthly spending limit. [categoryKey] is a [Category] name, or [OVERALL] for the whole month.
 */
@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val categoryKey: String,
    val limitMinor: Long,
    val updatedAt: Long = System.currentTimeMillis(),
) {
    companion object {
        const val OVERALL = "__OVERALL__"

        fun keyFor(category: Category?): String = category?.name ?: OVERALL
    }
}
