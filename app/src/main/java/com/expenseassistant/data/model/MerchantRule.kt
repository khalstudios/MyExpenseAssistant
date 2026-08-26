package com.expenseassistant.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Learned mapping created whenever the user re-categorises a merchant.
 * These take priority over the built-in keyword rules.
 */
@Entity(tableName = "merchant_rules")
data class MerchantRule(
    @PrimaryKey val merchantKey: String,
    val category: Category,
    val hitCount: Int = 1,
    val updatedAt: Long = System.currentTimeMillis(),
)
