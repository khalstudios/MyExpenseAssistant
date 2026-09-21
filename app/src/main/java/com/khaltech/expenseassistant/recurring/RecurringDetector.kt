package com.khaltech.expenseassistant.recurring

import com.khaltech.expenseassistant.categorize.Categorizer
import com.khaltech.expenseassistant.data.model.Category
import com.khaltech.expenseassistant.data.model.Direction
import com.khaltech.expenseassistant.data.model.TransactionEntity
import kotlin.math.abs

enum class Cadence(val label: String, val days: Int, val tolerance: Int) {
    WEEKLY("Weekly", 7, 2),
    MONTHLY("Monthly", 30, 6),
    QUARTERLY("Quarterly", 91, 12),
    YEARLY("Yearly", 365, 30);

    companion object {
        /** Monthly is the fallback: it is what most stored plans are, and a safe thing to guess. */
        fun fromName(value: String?): Cadence = entries.firstOrNull { it.name == value } ?: MONTHLY
    }
}

data class RecurringExpense(
    val merchant: String,
    val category: Category,
    val typicalAmountMinor: Long,
    val cadence: Cadence,
    val occurrences: Int,
    val lastSeenAt: Long,
    val nextExpectedAt: Long,
    /** Set when the user entered this themselves, which is also the row's edit and delete handle. */
    val manualId: Long? = null,
    /**
     * The normalised key detection groups by. Carried on the row so that deleting it dismisses
     * exactly what would otherwise be found again, rather than a display name that may be spelled
     * differently across the payments behind it.
     */
    val merchantKey: String = merchant.lowercase(),
) {
    /** True when this came from the history rather than from the user typing it in. */
    val isDetected: Boolean get() = manualId == null
}

/**
 * Finds subscriptions and other repeating charges by looking for a merchant paid at a
 * steady interval for a consistent amount.
 */
object RecurringDetector {

    /** Three consecutive payments are enough to call something recurring. */
    private const val MIN_OCCURRENCES = 3
    private const val AMOUNT_TOLERANCE = 0.25
    private const val DAY_MILLIS = 24L * 60 * 60 * 1000

    /**
     * Repeating charges in [transactions], dearest first.
     *
     * [dismissedAt] maps a merchant key to the moment the user deleted it from the card. Payments
     * up to that moment are ignored for that merchant, so the run that proved it the first time
     * cannot prove it again: the ordinary [MIN_OCCURRENCES] rule then requires three fresh
     * payments before it comes back. Deleting hides it for three more intervals, not forever —
     * a subscription that is genuinely still being paid should return eventually.
     */
    fun detect(
        transactions: List<TransactionEntity>,
        dismissedAt: Map<String, Long> = emptyMap(),
    ): List<RecurringExpense> = transactions
        .filter { it.direction == Direction.DEBIT }
        .groupBy { Categorizer.merchantKey(it.merchantRaw ?: it.merchant) ?: it.merchant.lowercase() }
        .mapNotNull { (key, payments) ->
            val since = dismissedAt[key]
            val considered = if (since == null) payments else payments.filter { it.occurredAt > since }
            considered.toRecurring(key)
        }
        .sortedByDescending { it.typicalAmountMinor }

    private fun List<TransactionEntity>.toRecurring(merchantKey: String): RecurringExpense? {
        if (size < MIN_OCCURRENCES) return null
        val ordered = sortedBy { it.occurredAt }
        // The cadences do not overlap, so the first that fits is the only one that can.
        return Cadence.entries.firstNotNullOfOrNull { ordered.trailingRunOf(it, merchantKey) }
    }

    /**
     * The most recent unbroken run of payments matching [cadence], or null when it is shorter than
     * [MIN_OCCURRENCES].
     *
     * Walking back from the latest payment, rather than demanding the whole history be regular, is
     * what lets a subscription surface on its third charge even when the same merchant was also paid
     * at odd times before it started. The run stops at the first payment that misses the interval or
     * the amount, so an irregular tail cannot disqualify a steady recent run.
     */
    private fun List<TransactionEntity>.trailingRunOf(cadence: Cadence, merchantKey: String): RecurringExpense? {
        val last = last()
        val anchorAmount = last.amountMinor
        if (anchorAmount <= 0) return null

        val gaps = mutableListOf<Int>()
        var index = lastIndex
        while (index > 0) {
            val previous = this[index - 1]
            val gap = ((this[index].occurredAt - previous.occurredAt) / DAY_MILLIS).toInt()
            if (gap <= 0 || abs(gap - cadence.days) > cadence.tolerance * 2) break
            if (abs(previous.amountMinor - anchorAmount).toDouble() / anchorAmount > AMOUNT_TOLERANCE) break
            gaps += gap
            index--
        }

        val occurrences = gaps.size + 1
        if (occurrences < MIN_OCCURRENCES) return null

        // The run's own rhythm can sit slightly off the nominal cadence; it still has to be close
        // enough that the next date it predicts is worth showing.
        val typicalGap = gaps.median()
        if (abs(typicalGap - cadence.days) > cadence.tolerance) return null

        val typicalAmount = subList(index, size).map { it.amountMinor }.median()
        return RecurringExpense(
            merchant = last.merchant,
            category = last.category,
            typicalAmountMinor = typicalAmount,
            cadence = cadence,
            occurrences = occurrences,
            lastSeenAt = last.occurredAt,
            nextExpectedAt = last.occurredAt + typicalGap * DAY_MILLIS,
            merchantKey = merchantKey,
        )
    }

    private fun List<Int>.median(): Int = sorted()[size / 2]

    @JvmName("medianLong")
    private fun List<Long>.median(): Long = sorted()[size / 2]
}
