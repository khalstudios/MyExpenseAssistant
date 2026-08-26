package com.expenseassistant.categorize

import com.expenseassistant.data.model.Category
import org.junit.Assert.assertEquals
import org.junit.Test

class MerchantKeywordsTest {

    @Test
    fun `matches known merchants`() {
        assertEquals(Category.FOOD_AND_DRINK, MerchantKeywords.match("Swiggy")?.first)
        assertEquals(Category.GROCERIES, MerchantKeywords.match("Swiggy Instamart")?.first)
        assertEquals(Category.TRANSPORT, MerchantKeywords.match("UBER INDIA SYSTEMS")?.first)
        assertEquals(Category.SHOPPING, MerchantKeywords.match("Amazon Seller Services")?.first)
        assertEquals(Category.FUEL, MerchantKeywords.match("HPCL Petrol Pump")?.first)
    }

    @Test
    fun `longest keyword wins`() {
        assertEquals(Category.GROCERIES, MerchantKeywords.match("blinkit")?.first)
    }

    @Test
    fun `normalises merchant keys`() {
        assertEquals("swiggy", Categorizer.merchantKey("Swiggy Private Limited"))
        assertEquals("rahulsharma", Categorizer.merchantKey("rahul.sharma@okhdfcbank"))
    }
}
