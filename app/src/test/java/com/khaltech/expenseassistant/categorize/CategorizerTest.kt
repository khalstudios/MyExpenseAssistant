package com.khaltech.expenseassistant.categorize

import com.khaltech.expenseassistant.data.local.MerchantRuleDao
import com.khaltech.expenseassistant.data.model.Category
import com.khaltech.expenseassistant.data.model.Direction
import com.khaltech.expenseassistant.data.model.MerchantRule
import com.khaltech.expenseassistant.parser.ParsedPayment
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategorizerTest {

    private val dao = FakeMerchantRuleDao()
    private var pro = true
    private val categorizer = Categorizer(dao, isPro = { pro })

    @Test
    fun `Pro files a garage under Vehicle Expense`() = runBlocking {
        pro = true

        assertEquals(Category.VEHICLE_EXPENSE, categorizer.categorize(payment("Shree Ganesh Garage", Direction.DEBIT)).category)
        assertEquals(Category.HOUSE_EXPENSE, categorizer.categorize(payment("Urban Company", Direction.DEBIT)).category)
    }

    @Test
    fun `without Pro a Pro category keyword files under its free stand-in`() = runBlocking {
        pro = false

        assertEquals(Category.TRANSPORT, categorizer.categorize(payment("Shree Ganesh Garage", Direction.DEBIT)).category)
        assertEquals(Category.BILLS_AND_UTILITIES, categorizer.categorize(payment("Urban Company", Direction.DEBIT)).category)
    }

    @Test
    fun `a category the user taught stands without Pro`() = runBlocking {
        pro = false
        categorizer.learn("Shree Ganesh Garage", Direction.DEBIT, Category.VEHICLE_EXPENSE)

        assertEquals(Category.VEHICLE_EXPENSE, categorizer.categorize(payment("Shree Ganesh Garage", Direction.DEBIT)).category)
    }

    @Test
    fun `spending picks up learned category, name and tags`() = runBlocking {
        categorizer.learn("ramesh.store@okaxis", Direction.DEBIT, Category.GROCERIES)
        categorizer.learnDisplayName("ramesh.store@okaxis", Direction.DEBIT, "Ramesh Kirana")
        categorizer.learnTags("ramesh.store@okaxis", Direction.DEBIT, listOf("monthly"))

        val guess = categorizer.categorize(payment("ramesh.store@okaxis", Direction.DEBIT))

        assertEquals(Category.GROCERIES, guess.category)
        assertEquals("Ramesh Kirana", guess.merchantDisplayName)
        assertEquals(listOf("monthly"), guess.tags)
    }

    @Test
    fun `a rule learned only from tags or a rename does not pin the merchant to Unknown`() = runBlocking {
        categorizer.learnTags("Blinkit", Direction.DEBIT, listOf("home"))
        categorizer.learnDisplayName("Blinkit", Direction.DEBIT, "Blinkit Groceries")

        val guess = categorizer.categorize(payment("Blinkit", Direction.DEBIT))

        assertEquals(Category.GROCERIES, guess.category)
        assertEquals("Blinkit Groceries", guess.merchantDisplayName)
        assertEquals(listOf("home"), guess.tags)
    }

    @Test
    fun `a taught category survives later tag and name edits`() = runBlocking {
        categorizer.learn("Blinkit", Direction.DEBIT, Category.HOUSE_EXPENSE)
        categorizer.learnTags("Blinkit", Direction.DEBIT, listOf("home"))

        assertEquals(Category.HOUSE_EXPENSE, categorizer.categorize(payment("Blinkit", Direction.DEBIT)).category)
    }

    @Test
    fun `a note stored by an earlier version is dropped when the rule is next saved`() = runBlocking {
        dao.upsert(MerchantRule(merchantKey = "swiggy", category = Category.FOOD_AND_DRINK, note = "Team lunch"))

        categorizer.learnTags("Swiggy", Direction.DEBIT, listOf("office"))

        assertNull(dao.find("swiggy")?.note)
    }

    @Test
    fun `income ignores merchant rules`() = runBlocking {
        categorizer.learn("Amazon", Direction.DEBIT, Category.SHOPPING)
        categorizer.learnDisplayName("Amazon", Direction.DEBIT, "Amazon Shopping")
        categorizer.learnTags("Amazon", Direction.DEBIT, listOf("gadgets"))

        val refund = categorizer.categorize(payment("Amazon", Direction.CREDIT))

        assertEquals(Category.INCOME, refund.category)
        assertNull(refund.merchantDisplayName)
        assertEquals(emptyList<String>(), refund.tags)
    }

    @Test
    fun `income teaches no merchant rules`() = runBlocking {
        categorizer.learn("Rahul Sharma", Direction.CREDIT, Category.FRIENDS)
        categorizer.learnDisplayName("Rahul Sharma", Direction.CREDIT, "Rahul")
        categorizer.learnTags("Rahul Sharma", Direction.CREDIT, listOf("split"))

        assertEquals(emptyList<MerchantRule>(), dao.all())
    }

    private fun payment(merchant: String, direction: Direction) = ParsedPayment(
        amountMinor = 50_000,
        currency = "INR",
        direction = direction,
        merchantRaw = merchant,
        referenceId = null,
        rawText = "Rs 500 $merchant",
        sourcePackage = "test",
        sourceApp = "Test",
        occurredAt = 0L,
    )

    private class FakeMerchantRuleDao : MerchantRuleDao {
        private val rules = LinkedHashMap<String, MerchantRule>()
        override suspend fun upsert(rule: MerchantRule) { rules[rule.merchantKey] = rule }
        override suspend fun upsertAll(rules: List<MerchantRule>) = rules.forEach { upsert(it) }
        override suspend fun find(key: String): MerchantRule? = rules[key]
        override suspend fun all(): List<MerchantRule> = rules.values.toList()
        override suspend fun deleteAll() = rules.clear()
    }
}
