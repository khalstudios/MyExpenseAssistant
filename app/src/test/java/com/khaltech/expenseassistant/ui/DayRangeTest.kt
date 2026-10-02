package com.khaltech.expenseassistant.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DayRangeTest {

    private fun at(day: Int, hour: Int, minute: Int = 0): Long = Calendar.getInstance().apply {
        set(2026, Calendar.OCTOBER, day, hour, minute, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    @Test
    fun `holds the whole of the day`() {
        val today = dayRange(at(2, 14, 4))
        assertTrue(at(2, 0) in today)
        assertTrue(at(2, 23, 59) in today)
    }

    @Test
    fun `leaves out a payment dated later`() {
        val today = dayRange(at(2, 14, 4))
        assertFalse(at(3, 0) in today)
        assertFalse(at(4, 7, 30) in today)
    }

    @Test
    fun `leaves out yesterday`() {
        assertFalse(at(1, 23, 59) in dayRange(at(2, 14, 4)))
    }
}
