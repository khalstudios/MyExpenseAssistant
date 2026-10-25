package com.khaltech.expenseassistant.data.model

import com.khaltech.expenseassistant.ui.SummaryFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InvestmentSplitTest {

    @Test
    fun `money into investments is not spending`() {
        val sip = tx(Category.INVESTMENTS)
        assertTrue(sip.isInvestment)
        assertFalse(sip.isSpending)
    }

    @Test
    fun `every other debit is spending`() {
        val lunch = tx(Category.FOOD_AND_DRINK)
        assertFalse(lunch.isInvestment)
        assertTrue(lunch.isSpending)
    }

    @Test
    fun `income is neither spending nor investment`() {
        // A redemption credited back under Investments is income, not a negative investment.
        val redemption = tx(Category.INVESTMENTS, Direction.CREDIT)
        assertFalse(redemption.isInvestment)
        assertFalse(redemption.isSpending)
    }

    @Test
    fun `a custom category is spending even when it carries investments underneath`() {
        val custom = tx(Category.INVESTMENTS).copy(customCategoryName = "Gold")
        assertFalse(custom.isInvestment)
        assertTrue(custom.isSpending)
    }

    @Test
    fun `summary flows split a month into income, spending and investments`() {
        val month = listOf(
            tx(Category.INCOME, Direction.CREDIT, 80_000_00),
            tx(Category.RENT, amountMinor = 25_000_00),
            tx(Category.GROCERIES, amountMinor = 5_000_00),
            tx(Category.INVESTMENTS, amountMinor = 15_000_00),
        )
        fun total(flow: SummaryFlow) = month.filter { flow.includes(it) }.sumOf { it.amountMinor }

        assertEquals(80_000_00L, total(SummaryFlow.INCOME))
        assertEquals(30_000_00L, total(SummaryFlow.SPENDING))
        assertEquals(15_000_00L, total(SummaryFlow.INVESTED))
    }

    private fun tx(
        category: Category,
        direction: Direction = Direction.DEBIT,
        amountMinor: Long = 1_000_00,
    ) = TransactionEntity(
        amountMinor = amountMinor,
        direction = direction,
        merchantRaw = null,
        merchant = category.displayName,
        category = category,
        categoryConfidence = 1f,
        sourcePackage = null,
        sourceApp = "test",
        captureSource = CaptureSource.NOTIFICATION,
        rawText = "",
        referenceId = null,
        occurredAt = 0L,
        dedupeKey = "${category.name}-$direction-$amountMinor",
    )
}
