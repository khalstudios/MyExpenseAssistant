package com.khaltech.expenseassistant.data.repo

import com.khaltech.expenseassistant.data.model.CaptureSource
import com.khaltech.expenseassistant.data.model.Category
import com.khaltech.expenseassistant.data.model.Direction
import com.khaltech.expenseassistant.data.model.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EntrySuggestionsTest {

    @Test
    fun `most used merchant comes first and carries its latest category and tags`() {
        val ranked = merchantSuggestions(
            listOf(
                entry("Blinkit", at = 1, category = Category.OTHER),
                entry("blinkit", at = 3, category = Category.GROCERIES, tags = listOf("home")),
                entry("Uber", at = 2),
            ),
        )

        assertEquals(listOf("blinkit", "Uber"), ranked.map { it.name })
        with(ranked.first()) {
            assertEquals(2, useCount)
            assertEquals(Category.GROCERIES, category)
            assertEquals(listOf("home"), tags)
        }
    }

    @Test
    fun `typing offers the most used name with a word starting with the text`() {
        val ranked = merchantSuggestions(
            listOf(
                entry("Swiggy Instamart", at = 1),
                entry("Swiggy Instamart", at = 2),
                entry("Instagram Ads", at = 3),
            ),
        )

        assertEquals("Swiggy Instamart", bestSuggestion("insta", ranked) { it.name }?.name)
        assertNull(bestSuggestion("gram", ranked) { it.name })
    }

    @Test
    fun `the list holds every match up to the limit, most used first`() {
        val ranked = merchantSuggestions(
            listOf(
                entry("Dzire Petrol", at = 1),
                entry("Dzire Petrol", at = 2),
                entry("Dzire Washing", at = 3),
                entry("Dzire EMI", at = 4),
                entry("Uber", at = 5),
            ),
        )

        assertEquals(
            listOf("Dzire Petrol", "Dzire EMI"),
            matchingSuggestions("dzire", ranked, limit = 2) { it.name }.map { it.name },
        )
    }

    @Test
    fun `a name already in the field is not offered back`() {
        val ranked = merchantSuggestions(listOf(entry("Uber", at = 1)))

        assertNull(bestSuggestion("uber", ranked) { it.name })
    }

    @Test
    fun `an empty field offers the most used entry`() {
        val ranked = merchantSuggestions(listOf(entry("Uber", at = 1), entry("Ola", at = 2), entry("Ola", at = 3)))

        assertEquals("Ola", bestSuggestion("", ranked) { it.name }?.name)
    }

    @Test
    fun `notes rank by use and skip blanks`() {
        val ranked = noteSuggestions(
            listOf(
                entry("A", at = 1, note = "Office lunch"),
                entry("B", at = 2, note = "office lunch"),
                entry("C", at = 3, note = "Fuel"),
                entry("D", at = 4, note = "  "),
            ),
        )

        assertEquals(listOf("office lunch", "Fuel"), ranked.map { it.text })
        assertEquals("office lunch", bestSuggestion("lun", ranked) { it.text }?.text)
    }

    @Test
    fun `merging tags keeps existing ones and skips case duplicates`() {
        assertEquals(listOf("Home", "rent"), mergeTags(listOf("Home"), listOf("home", "rent", "Rent")))
    }

    private fun entry(
        merchant: String,
        at: Long,
        category: Category = Category.OTHER,
        tags: List<String> = emptyList(),
        note: String? = null,
    ) = TransactionEntity(
        amountMinor = 10_000,
        direction = Direction.DEBIT,
        merchantRaw = merchant,
        merchant = merchant,
        category = category,
        categoryConfidence = 1f,
        sourcePackage = null,
        sourceApp = "test",
        captureSource = CaptureSource.NOTIFICATION,
        rawText = "",
        referenceId = null,
        occurredAt = at,
        dedupeKey = "$merchant-$at",
        description = note,
        tags = tags,
    )
}
