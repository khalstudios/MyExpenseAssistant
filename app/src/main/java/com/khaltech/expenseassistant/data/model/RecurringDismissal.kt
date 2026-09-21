package com.khaltech.expenseassistant.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A recurring payment the user deleted, and the moment they did it.
 *
 * Detection reads the transaction history, so a merchant deleted from the recurring card would be
 * found again on the very next pass — the payments that proved it are still there. Recording the
 * dismissal lets detection ignore everything up to this point and start counting afresh, so the
 * card only offers the merchant again once it has earned its place a second time.
 *
 * [merchantKey] is the normalised key the detector groups by, not the display name, so a merchant
 * whose name is written differently across payments is still the same dismissal.
 */
@Entity(tableName = "recurring_dismissals")
data class RecurringDismissal(
    @PrimaryKey val merchantKey: String,
    val dismissedAt: Long = System.currentTimeMillis(),
)
