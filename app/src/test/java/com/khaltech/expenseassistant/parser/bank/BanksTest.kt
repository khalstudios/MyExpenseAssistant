package com.khaltech.expenseassistant.parser.bank

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BanksTest {

    @Test
    fun `reads dlt headers with and without the traffic suffix`() {
        assertEquals("HDFC Bank", Banks.fromSender("VM-HDFCBK-S")?.name)
        assertEquals("HDFC Bank", Banks.fromSender("AD-HDFCBK")?.name)
        assertEquals("ICICI Bank", Banks.fromSender("ICICIT")?.name)
        assertEquals("State Bank of India", Banks.fromSender("JD-SBIUPI-T")?.name)
    }

    @Test
    fun `falls back to a saved contact name`() {
        assertEquals("Axis Bank", Banks.fromSender("Axis Bank")?.name)
        assertNull(Banks.fromSender("Mom"))
    }

    @Test
    fun `longer bank names win over the shorter names they contain`() {
        assertEquals("Central Bank of India", Banks.fromText("-Central Bank of India")?.name)
        assertEquals("South Indian Bank", Banks.fromText("South Indian Bank: A/c debited")?.name)
        assertEquals("SBI Card", Banks.fromText("spent on your SBI Credit Card ending 1234")?.name)
        assertEquals("Bank of India", Banks.fromText("-Bank of India")?.name)
    }

    @Test
    fun `sender codes are unique across banks`() {
        val codes = Banks.all.flatMap { it.senderCodes }
        assertEquals(codes.size, codes.toSet().size)
        assertTrue(Banks.all.size >= 40)
    }

    @Test
    fun `only the promotional suffix marks a promotional header`() {
        assertTrue(Banks.isPromotionalSender("VM-HDFCBK-P"))
        assertFalse(Banks.isPromotionalSender("VM-HDFCBK-S"))
        assertFalse(Banks.isPromotionalSender("VM-HDFCBK"))
    }
}
