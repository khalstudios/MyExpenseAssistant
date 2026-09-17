package com.khaltech.expenseassistant.ui

import com.khaltech.expenseassistant.data.model.Category
import org.junit.Assert.assertTrue
import org.junit.Test

class SpendingTipTest {

    private fun tips(
        todaySpendMinor: Long,
        dailyBudgetMinor: Long,
        byCategory: List<Pair<Category, Long>> = emptyList(),
        count: Int = 1,
    ) = spendingTips(todaySpendMinor, dailyBudgetMinor, byCategory, count)

    @Test
    fun `an unspent day always has something to say`() {
        val result = tips(todaySpendMinor = 0, dailyBudgetMinor = 500_00)
        assertTrue(result.isNotEmpty())
        assertTrue(result.toString(), result.any { it.contains("clean slate") })
    }

    @Test
    fun `an unspent day without a budget still returns a tip`() {
        assertTrue(tips(todaySpendMinor = 0, dailyBudgetMinor = 0).isNotEmpty())
    }

    @Test
    fun `going over the budget says so`() {
        val result = tips(todaySpendMinor = 700_00, dailyBudgetMinor = 500_00)
        assertTrue(result.toString(), result.any { it.contains("over today's budget") })
    }

    @Test
    fun `nearly spent budget warns rather than congratulates`() {
        // 460 of 500 leaves 40, inside the one-fifth cushion.
        val result = tips(
            todaySpendMinor = 460_00,
            dailyBudgetMinor = 500_00,
            byCategory = listOf(Category.FOOD_AND_DRINK to 460_00),
        )
        assertTrue(result.toString(), result.any { it.contains("Only") })
        assertTrue(result.toString(), result.none { it.contains("doing well") })
    }

    @Test
    fun `a comfortable day reads positively`() {
        val result = tips(todaySpendMinor = 100_00, dailyBudgetMinor = 500_00)
        assertTrue(result.toString(), result.any { it.contains("doing well") })
    }

    @Test
    fun `the top category is called out by name`() {
        val result = tips(
            todaySpendMinor = 300_00,
            dailyBudgetMinor = 500_00,
            byCategory = listOf(Category.FOOD_AND_DRINK to 200_00, Category.TRAVEL to 100_00),
            count = 2,
        )
        assertTrue(result.toString(), result.any { it.contains(Category.FOOD_AND_DRINK.displayName) })
    }

    @Test
    fun `without a budget it invites you to set one`() {
        val result = tips(todaySpendMinor = 300_00, dailyBudgetMinor = 0)
        assertTrue(result.toString(), result.any { it.contains("Set a daily budget") })
    }

    @Test
    fun `a single spend is not pluralised`() {
        val result = tips(todaySpendMinor = 300_00, dailyBudgetMinor = 500_00, count = 1)
        assertTrue(result.toString(), result.any { it.contains("1 spend.") })
    }
}
