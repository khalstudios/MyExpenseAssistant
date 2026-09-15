package com.khaltech.expenseassistant.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Learned mapping created whenever the user re-categorises or edits a merchant's spending
 * transaction. Alongside the category we remember the display name and tags the user last saved,
 * so the next payment to the same merchant arrives already filled in. Income neither teaches nor
 * uses these rules.
 * These take priority over the built-in keyword rules. A [category] of OTHER means only a name or
 * tags were learned, so the category still comes from the keyword rules.
 *
 * [note] is no longer learned or applied; the column stays so older databases and backups load.
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
