package com.khaltech.expenseassistant.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryTest {

    @Test
    fun `split categories read back as the half they moved to`() {
        assertEquals(Category.FRIENDS, Category.fromName("FRIENDS_AND_FAMILY"))
        assertEquals(Category.GIFTS, Category.fromName("GIFTS_AND_DONATION"))
        assertEquals(Category.HOUSE_EXPENSE, Category.fromName("MAINTENANCE"))
    }

    @Test
    fun `every current category reads back as itself`() {
        Category.entries.forEach { assertEquals(it, Category.fromName(it.name)) }
    }

    @Test
    fun `budget keys move to the successor and leave everything else alone`() {
        assertEquals("FRIENDS", Category.currentKey("FRIENDS_AND_FAMILY"))
        assertEquals("GIFTS", Category.currentKey("GIFTS_AND_DONATION"))
        assertEquals("OVERALL", Category.currentKey("OVERALL"))
        assertEquals("RENT", Category.currentKey("RENT"))
    }

    @Test
    fun `a choice made for a retired category moves to its successor`() {
        val current = Category.currentKeys(mapOf("FRIENDS_AND_FAMILY" to "groups", "RENT" to "home"))

        assertEquals(mapOf("FRIENDS" to "groups", "RENT" to "home"), current)
    }

    @Test
    fun `a choice made for the successor itself wins over the retired one`() {
        val current = Category.currentKeys(mapOf("GIFTS_AND_DONATION" to "#FF0000", "GIFTS" to "#00FF00"))

        assertEquals(mapOf("GIFTS" to "#00FF00"), current)
    }
}
