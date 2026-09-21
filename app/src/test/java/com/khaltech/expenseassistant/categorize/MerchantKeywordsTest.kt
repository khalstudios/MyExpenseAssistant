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
     * A category behind Pro that the categoriser can reach for a free user is not behind Pro: it
     * lands on their transactions by itself, which unlocks it for them and makes the paywall wrong.
     * So every Pro category a keyword names needs a free stand-in to file under instead.
     */
    @Test
    fun `every extended category a keyword reaches has a free fallback`() {
        val uncovered = MerchantKeywords.rules.keys.filter { it.isExtended && it !in MerchantKeywords.freeFallback }

        assertEquals(
            "These are sold as Pro but the categoriser would assign them to free users. Add them " +
                "to MerchantKeywords.freeFallback, drop the keyword rules, or move them into " +
                "Category.Default.",
            emptyList<Category>(),
            uncovered,
        )
    }

    @Test
    fun `every free fallback is itself free`() {
        val lockedFallbacks = MerchantKeywords.freeFallback.filterValues { it.isExtended }

        assertEquals(emptyMap<Category, Category>(), lockedFallbacks)
    }

    @Test
    fun `normalises merchant keys`() {
        assertEquals("swiggy", Categorizer.merchantKey("Swiggy Private Limited"))
        assertEquals("rahulsharma", Categorizer.merchantKey("rahul.sharma@okhdfcbank"))
    }
}
