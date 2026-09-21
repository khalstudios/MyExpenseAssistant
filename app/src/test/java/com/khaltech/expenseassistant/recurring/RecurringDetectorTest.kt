package com.khaltech.expenseassistant.recurring

import com.khaltech.expenseassistant.data.model.CaptureSource
import com.khaltech.expenseassistant.data.model.Category
import com.khaltech.expenseassistant.data.model.Direction
import com.khaltech.expenseassistant.data.model.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val DAY = 24L * 60 * 60 * 1000
private const val NOW = 1_800_000_000_000L

class RecurringDetectorTest {

    @Test
    fun `three monthly payments are enough`() {
        val found = RecurringDetector.detect(monthly(count = 3))

        assertEquals(1, found.size)
        assertEquals(Cadence.MONTHLY, found.first().cadence)
        assertEquals(3, found.first().occurrences)
        assertEquals(49900L, found.first().typicalAmountMinor)
    }

    @Test
    fun `two payments are not a pattern`() {
        assertTrue(RecurringDetector.detect(monthly(count = 2)).isEmpty())
    }

    @Test
    fun `an older irregular payment does not disqualify a steady recent run`() {
        val noise = payment(amountMinor = 12_000, daysAgo = 200)

        val found = RecurringDetector.detect(monthly(count = 3) + noise)

        assertEquals(1, found.size)
        // The run is the three recent charges; the stray one is left outside it.
        assertEquals(3, found.first().occurrences)
    }

    @Test
    fun `an amount that jumps ends the run`() {
        // Two charges at the usual price, then a third at nearly double: only two consecutive
        // payments actually match, which is one short.
        val transactions = listOf(
            payment(amountMinor = 49_900, daysAgo = 0),
            payment(amountMinor = 49_900, daysAgo = 30),
            payment(amountMinor = 99_000, daysAgo = 60),
        )

        assertTrue(RecurringDetector.detect(transactions).isEmpty())
    }

    @Test
    fun `weekly and monthly are told apart`() {
        val weekly = (0 until 3).map { payment(amountMinor = 20_000, daysAgo = it * 7) }

        assertEquals(Cadence.WEEKLY, RecurringDetector.detect(weekly).single().cadence)
    }

    @Test
    fun `income is ignored`() {
        val salary = (0 until 4).map {
            payment(amountMinor = 5_000_000, daysAgo = it * 30, direction = Direction.CREDIT)
        }

        assertTrue(RecurringDetector.detect(salary).isEmpty())
    }

    @Test
    fun `a dismissed merchant is not detected again straight away`() {
        val transactions = monthly(count = 4)
        val key = RecurringDetector.detect(transactions).single().merchantKey

        val found = RecurringDetector.detect(transactions, mapOf(key to NOW))

        assertTrue(found.isEmpty())
    }

    @Test
    fun `a dismissed merchant comes back after three fresh payments`() {
        // Dismissed a year ago, then paid monthly three times since.
        val dismissedAt = NOW - 365 * DAY
        val since = (0 until 3).map { payment(amountMinor = 49_900, daysAgo = it * 30) }
        val before = (0 until 4).map { payment(amountMinor = 49_900, daysAgo = 400 + it * 30) }
        val key = RecurringDetector.detect(since).first().merchantKey

        val found = RecurringDetector.detect(before + since, mapOf(key to dismissedAt))

        assertEquals(1, found.size)
        // Only the payments after the dismissal count towards it.
        assertEquals(3, found.first().occurrences)
    }

    @Test
    fun `two fresh payments after a dismissal are still not enough`() {
        val dismissedAt = NOW - 365 * DAY
        val since = (0 until 2).map { payment(amountMinor = 49_900, daysAgo = it * 30) }
        val before = (0 until 4).map { payment(amountMinor = 49_900, daysAgo = 400 + it * 30) }
        val key = RecurringDetector.detect(before).first().merchantKey

        assertTrue(RecurringDetector.detect(before + since, mapOf(key to dismissedAt)).isEmpty())
    }

    private fun monthly(count: Int): List<TransactionEntity> =
        (0 until count).map { payment(amountMinor = 49_900, daysAgo = it * 30) }

    private fun payment(
        amountMinor: Long,
        daysAgo: Int,
        merchant: String = "Netflix",
        direction: Direction = Direction.DEBIT,
    ) = TransactionEntity(
        amountMinor = amountMinor,
        direction = direction,
        merchantRaw = merchant,
        merchant = merchant,
        category = Category.OTHER,
        categoryConfidence = 1f,
        sourcePackage = null,
        sourceApp = "test",
        captureSource = CaptureSource.MANUAL,
        rawText = "",
        referenceId = null,
        occurredAt = NOW - daysAgo * DAY,
        dedupeKey = "$merchant-$daysAgo-$amountMinor",
    )
}
