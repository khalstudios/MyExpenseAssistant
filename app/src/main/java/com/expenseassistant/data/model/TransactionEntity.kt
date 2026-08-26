package com.expenseassistant.data.model

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
) {
    @get:Ignore
    val amount: Double get() = amountMinor / 100.0
}
