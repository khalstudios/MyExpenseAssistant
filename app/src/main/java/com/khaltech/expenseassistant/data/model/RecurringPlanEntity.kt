package com.khaltech.expenseassistant.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A repeating payment the user entered themselves.
 *
 * The detector can only find what has already been paid three times, which leaves out the rent
 * standing instruction set up last week and anything paid outside the phone. This is the manual
 * counterpart: it records the same facts the detector would have worked out, so the two can be
 * shown as one list.
 *
 * [cadence] holds a [com.khaltech.expenseassistant.recurring.Cadence] name, stored as text rather
 * than the enum so that retiring a cadence later cannot make the table unreadable.
 */
@Entity(tableName = "recurring_plans")
data class RecurringPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchant: String,
    val category: Category,
    val amountMinor: Long,
    val cadence: String,
    /** When the next payment falls due; reading rolls it forward as dates pass. */
    val nextDueAt: Long,
    val createdAt: Long = System.currentTimeMillis(),
)
