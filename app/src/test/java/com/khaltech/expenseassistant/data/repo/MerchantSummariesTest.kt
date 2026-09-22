package com.khaltech.expenseassistant.data.repo

import com.khaltech.expenseassistant.data.model.CaptureSource
import com.khaltech.expenseassistant.data.model.Category
import com.khaltech.expenseassistant.data.model.Direction
import com.khaltech.expenseassistant.data.model.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class MerchantSummariesTest {

    @Test
    fun `payments to one merchant group under its key, most-paid merchant first`() {
        val summaries = merchantSummaries(
            listOf(
                payment("ZOMATO LTD", at = 1, amountMinor = 10_000),
                payment("Zomato", at = 2, amountMinor = 20_000),
                payment("Zomato", at = 3, amountMinor = 30_000, category = Category.FOOD_AND_DRINK),
                payment("Uber", at = 4),
            ),
        )

        assertEquals(listOf("zomato", "uber"), summaries.map { it.key })
        with(summaries.first()) {
            assertEquals(3, paymentCount)
            assertEquals(60_000L, spentMinor)
            assertEquals(3L, lastPaidAt)
            assertEquals(Category.FOOD_AND_DRINK, category)
        }
    }

    @Test
    fun `a merchant is shown by its latest payment's name`() {
        val summary = merchantSummaries(
            listOf(payment("Zomato", at = 1), payment("Zomato", at = 2, shownAs = "Dinner")),
        ).single()

        assertEquals("Dinner", summary.name)
    }

    @Test
    fun `income and payments naming no merchant are left out`() {
        val summaries = merchantSummaries(
            listOf(payment("Employer", at = 1, direction = Direction.CREDIT), payment(null, at = 2)),
        )

        assertEquals(emptyList<MerchantSummary>(), summaries)
    }

    private fun payment(
        merchant: String?,
        at: Long,
        amountMinor: Long = 10_000,
        direction: Direction = Direction.DEBIT,
        category: Category = Category.OTHER,
        shownAs: String? = null,
    ) = TransactionEntity(
        amountMinor = amountMinor,
        direction = direction,
        merchantRaw = merchant,
        merchant = shownAs ?: merchant ?: "ATM withdrawal",
        category = category,
        categoryConfidence = 1f,
        sourcePackage = null,
        sourceApp = "test",
        captureSource = CaptureSource.NOTIFICATION,
        rawText = "",
        referenceId = null,
        occurredAt = at,
        dedupeKey = "$merchant-$at",
    )
}
