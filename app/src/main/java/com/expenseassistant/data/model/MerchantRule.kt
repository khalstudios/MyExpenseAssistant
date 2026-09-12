package com.expenseassistant.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Learned mapping created whenever the user re-categorises or edits a merchant's transaction.
 * Alongside the category we remember the display name, tags and note the user last saved, so the
 * next payment to the same merchant arrives already filled in.
 * These take priority over the built-in keyword rules.
 */
@Entity(tableName = "merchant_rules")
data class MerchantRule(
    @PrimaryKey val merchantKey: String,
    val category: Category,
    val displayName: String? = null,
    val tags: List<String> = emptyList(),
    val note: String? = null,
    val hitCount: Int = 1,
    val updatedAt: Long = System.currentTimeMillis(),
)
