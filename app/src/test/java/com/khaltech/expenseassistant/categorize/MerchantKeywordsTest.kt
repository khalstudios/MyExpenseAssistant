package com.khaltech.expenseassistant.categorize

import com.khaltech.expenseassistant.data.model.Category
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
        assertEquals(Category.HOUSE_EXPENSE, MerchantKeywords.match("Urban Company")?.first)
        assertEquals(Category.HOUSE_EXPENSE, MerchantKeywords.match("Sharma Pest Control")?.first)
        assertEquals(Category.VEHICLE_EXPENSE, MerchantKeywords.match("GoMechanic Car Service")?.first)
        assertEquals(Category.VEHICLE_EXPENSE, MerchantKeywords.match("Shree Ganesh Garage")?.first)
    }

    @Test
    fun `vehicle brands beat shorter keywords from other categories`() {
        assertEquals(Category.VEHICLE_EXPENSE, MerchantKeywords.match("Apollo Tyres Ltd")?.first)
        assertEquals(Category.VEHICLE_EXPENSE, MerchantKeywords.match("Bajaj Auto Service")?.first)
        assertEquals(Category.VEHICLE_EXPENSE, MerchantKeywords.match("Ola Electric Mobility")?.first)
        assertEquals(Category.HEALTH, MerchantKeywords.match("Apollo Pharmacy")?.first)
    }

    @Test
    fun `longest keyword wins`() {
        assertEquals(Category.GROCERIES, MerchantKeywords.match("blinkit")?.first)
    }

    /**
     * A category behind Pro that the categoriser can reach anyway is not behind Pro: it lands on a
     * free user's transactions by itself, which unlocks it for them and makes the paywall wrong.
     * This is why Travel, House Expense and Vehicle Expense sit in the default set.
     */
    @Test
    fun `no extended category can be reached by a keyword rule`() {
        val reachable = MerchantKeywords.rules.keys.filter { it.isExtended }

        assertEquals(
            "These are sold as Pro but the categoriser assigns them on its own. Either drop the " +
                "keyword rules or move them into Category.Default.",
            emptyList<Category>(),
            reachable,
        )
    }

    @Test
    fun `normalises merchant keys`() {
        assertEquals("swiggy", Categorizer.merchantKey("Swiggy Private Limited"))
        assertEquals("rahulsharma", Categorizer.merchantKey("rahul.sharma@okhdfcbank"))
    }
}
