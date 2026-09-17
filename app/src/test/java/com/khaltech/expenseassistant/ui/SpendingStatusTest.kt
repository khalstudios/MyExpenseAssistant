package com.khaltech.expenseassistant.ui

import com.khaltech.expenseassistant.ui.insights.BudgetProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpendingStatusTest {

    /** Half way through the month, so spending more than half the limit is ahead of pace. */
    private fun budget(limitMinor: Long, spentMinor: Long, pace: Float = 0.5f) =
        BudgetProgress(category = null, limitMinor = limitMinor, spentMinor = spentMinor, paceFraction = pace)

    @Test
    fun `no budget means no status`() {
        assertNull(spendingStatus(todaySpendMinor = 50_00, dailyShareMinor = 0, budget = null, daysLeft = 10))
    }

    @Test
    fun `a zero limit means no status`() {
        val status = spendingStatus(
            todaySpendMinor = 50_00,
            dailyShareMinor = 0,
            budget = budget(limitMinor = 0, spentMinor = 0),
            daysLeft = 10,
        )
        assertNull(status)
    }

    @Test
    fun `spending past the limit reads as over budget`() {
        val status = spendingStatus(
            todaySpendMinor = 10_00,
            dailyShareMinor = 100_00,
            budget = budget(limitMinor = 3000_00, spentMinor = 3200_00),
            daysLeft = 5,
        )!!
        assertEquals(SpendingMood.OVER, status.mood)
        assertEquals("Over budget", status.headline)
        assertTrue(status.detail, status.detail.contains("with 5 days to go"))
    }

    @Test
    fun `outrunning the pace reads as spending fast`() {
        val status = spendingStatus(
            todaySpendMinor = 10_00,
            dailyShareMinor = 100_00,
            budget = budget(limitMinor = 3000_00, spentMinor = 2400_00),
            daysLeft = 15,
        )!!
        assertEquals(SpendingMood.WATCH, status.mood)
        assertEquals("Spending fast", status.headline)
    }

    @Test
    fun `a big day against an on-pace month reads as a heavy day`() {
        val status = spendingStatus(
            todaySpendMinor = 400_00,
            dailyShareMinor = 100_00,
            budget = budget(limitMinor = 3000_00, spentMinor = 1000_00),
            daysLeft = 15,
        )!!
        assertEquals(SpendingMood.WATCH, status.mood)
        assertEquals("Heavy day", status.headline)
    }

    @Test
    fun `an unspent day on an on-pace month reads as good`() {
        val status = spendingStatus(
            todaySpendMinor = 0,
            dailyShareMinor = 100_00,
            budget = budget(limitMinor = 3000_00, spentMinor = 1000_00),
            daysLeft = 15,
        )!!
        assertEquals(SpendingMood.GOOD, status.mood)
        assertEquals("Nothing spent yet today", status.headline)
    }

    @Test
    fun `within the daily share and the monthly pace reads as good`() {
        val status = spendingStatus(
            todaySpendMinor = 40_00,
            dailyShareMinor = 100_00,
            budget = budget(limitMinor = 3000_00, spentMinor = 1000_00),
            daysLeft = 15,
        )!!
        assertEquals(SpendingMood.GOOD, status.mood)
        assertEquals("Looking good", status.headline)
    }

    @Test
    fun `the final day drops the countdown`() {
        val status = spendingStatus(
            todaySpendMinor = 40_00,
            dailyShareMinor = 100_00,
            budget = budget(limitMinor = 3000_00, spentMinor = 1000_00, pace = 1f),
            daysLeft = 0,
        )!!
        assertTrue(status.detail, status.detail.contains("on the last day of the month"))
    }

    @Test
    fun `a single remaining day is not pluralised`() {
        val status = spendingStatus(
            todaySpendMinor = 40_00,
            dailyShareMinor = 100_00,
            budget = budget(limitMinor = 3000_00, spentMinor = 1000_00, pace = 0.9f),
            daysLeft = 1,
        )!!
        assertTrue(status.detail, status.detail.contains("with 1 day to go"))
    }
}
