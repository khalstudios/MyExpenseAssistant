package com.expenseassistant.ui.detail

import com.expenseassistant.data.model.Category
import com.expenseassistant.data.model.Direction
import com.expenseassistant.data.model.PaymentMode

/** Everything the detail screen can change, committed in one go by the save action. */
data class TransactionEdits(
    val amountMinor: Long,
    val direction: Direction,
    val merchant: String,
    val occurredAt: Long,
    val category: Category,
    val customCategoryName: String?,
    val customCategoryColor: String?,
    val customCategoryIcon: String?,
    val paymentMode: PaymentMode,
    val description: String?,
    val tags: List<String>,
)
