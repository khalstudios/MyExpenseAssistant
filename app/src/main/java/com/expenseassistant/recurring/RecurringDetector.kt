package com.expenseassistant.recurring

import com.expenseassistant.categorize.Categorizer
import com.expenseassistant.data.model.Category
import com.expenseassistant.data.model.Direction
import com.expenseassistant.data.model.TransactionEntity
import kotlin.math.abs

enum class Cadence(val label: String, val days: Int, val tolerance: Int) {
    WEEKLY("Weekly", 7, 2),
    MONTHLY("Monthly", 30, 6),
    QUARTERLY("Quarterly", 91, 12),
    YEARLY("Yearly", 365, 30),
}

data class RecurringExpense(
    val merchant: String,
    val category: Category,
    val typicalAmountMinor: Long,
    val cadence: Cadence,
    val occurrences: Int,
    val lastSeenAt: Long,
    val nextExpectedAt: Long,
)

/**
 * Finds subscriptions and other repeating charges by looking for a merchant paid at a
 * steady interval for a consistent amount.
 */
object RecurringDetector {

    private const val MIN_OCCURRENCES = 3
    private const val AMOUNT_TOLERANCE = 0.25
    private const val DAY_MILLIS = 24L * 60 * 60 * 1000

    fun detect(transactions: List<TransactionEntity>): List<RecurringExpense> = transactions
        .filter { it.direction == Direction.DEBIT }
        .groupBy { Categorizer.merchantKey(it.merchantRaw ?: it.merchant) ?: it.merchant.lowercase() }
        .values
        .mapNotNull { it.toRecurring() }
        .sortedByDescending { it.typicalAmountMinor }

    private fun List<TransactionEntity>.toRecurring(): RecurringExpense? {
        if (size < MIN_OCCURRENCES) return null

        val ordered = sortedBy { it.occurredAt }
        val gapsInDays = ordered.zipWithNext { a, b -> ((b.occurredAt - a.occurredAt) / DAY_MILLIS).toInt() }
        if (gapsInDays.any { it <= 0 }) return null

        val typicalGap = gapsInDays.median()
        val cadence = Cadence.entries.firstOrNull { abs(typicalGap - it.days) <= it.tolerance } ?: return null
        if (gapsInDays.any { abs(it - cadence.days) > cadence.tolerance * 2 }) return null

        val amounts = ordered.map { it.amountMinor }
        val typicalAmount = amounts.median()
        if (typicalAmount <= 0) return null
        if (amounts.any { abs(it - typicalAmount).toDouble() / typicalAmount > AMOUNT_TOLERANCE }) return null

        val last = ordered.last()
        return RecurringExpense(
            merchant = last.merchant,
            category = last.category,
            typicalAmountMinor = typicalAmount,
            cadence = cadence,
            occurrences = size,
            lastSeenAt = last.occurredAt,
            nextExpectedAt = last.occurredAt + typicalGap * DAY_MILLIS,
        )
    }

    private fun List<Int>.median(): Int = sorted()[size / 2]

    @JvmName("medianLong")
    private fun List<Long>.median(): Long = sorted()[size / 2]
}
