package com.khaltech.expenseassistant.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Amounts are stored in minor units (paise) to avoid floating point drift.
 */
@Entity(
    tableName = "transactions",
    indices = [
        // Unique: the last line of defence against one payment being captured twice.
        Index(value = ["dedupeKey"], unique = true),
        Index(value = ["occurredAt"]),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountMinor: Long,
    val currency: String = "INR",
    val direction: Direction,
    val merchantRaw: String?,
    val merchant: String,
    val category: Category,
    val categoryConfidence: Float,
    val sourcePackage: String?,
    val sourceApp: String,
    val captureSource: CaptureSource,
    val rawText: String,
    val referenceId: String?,
    val occurredAt: Long,
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "note") val description: String? = null,
    val tags: List<String> = emptyList(),
    val paymentMode: PaymentMode = PaymentMode.UNKNOWN,
    val userCorrected: Boolean = false,
    val dedupeKey: String,
    /** Set together, non-null only when the user picked a category they created themselves. */
    val customCategoryName: String? = null,
    val customCategoryColor: String? = null,
    val customCategoryIcon: String? = null,
    // What the bank alert stated; null for app notifications, screen captures and manual entries.
    val bankName: String? = null,
    val accountType: AccountType? = null,
    val accountLast4: String? = null,
    val transactionType: TransactionType? = null,
    val availableBalanceMinor: Long? = null,
) {
    @get:Ignore
    val amount: Double get() = amountMinor / 100.0

    /**
     * The note the user actually wrote, if any. Capture parks the raw merchant string in
     * [description] when it differs from the display name, and echoing that back is not a note.
     */
    @get:Ignore
    val userNote: String?
        get() {
            val trimmed = description?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            return trimmed.takeUnless {
                it.equals(merchantRaw?.trim(), ignoreCase = true) ||
                    it.equals(merchant.trim(), ignoreCase = true)
            }
        }

    /**
     * What a transaction list row leads with: the note the user wrote, when there is one, and the
     * merchant name otherwise. A note is the more useful label once the user has bothered to add
     * one, and the merchant is still shown on the detail screen.
     */
    @get:Ignore
    val displayTitle: String get() = userNote ?: merchant
}
