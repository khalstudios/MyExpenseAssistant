package com.expenseassistant.parser

import com.expenseassistant.data.model.Direction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PaymentTextParserTest {

    private val gpay = "com.google.android.apps.nbu.paisa.user"

    @Test
    fun `parses gpay paid notification`() {
        val result = PaymentTextParser.parse("You paid ₹249.50 to Swiggy", gpay)
        assertNotNull(result)
        assertEquals(24950L, result!!.amountMinor)
        assertEquals(Direction.DEBIT, result.direction)
        assertEquals("Swiggy", result.merchantRaw)
    }

    @Test
    fun `parses bank debit sms with reference`() {
        val text = "Rs.1,250.00 debited from A/c XX1234 to UBER INDIA on 12-05-25. UPI Ref No 512345678901"
        val result = PaymentTextParser.parse(text, "com.google.android.apps.messaging")
        assertNotNull(result)
        assertEquals(125000L, result!!.amountMinor)
        assertEquals(Direction.DEBIT, result.direction)
        assertEquals("512345678901", result.referenceId)
    }

    @Test
    fun `parses credit`() {
        val result = PaymentTextParser.parse("₹5,000 credited to your account from ACME PAYROLL", gpay)
        assertNotNull(result)
        assertEquals(Direction.CREDIT, result!!.direction)
        assertEquals(500000L, result.amountMinor)
    }

    @Test
    fun `ignores failed and pending payments`() {
        assertNull(PaymentTextParser.parse("Your payment of ₹500 to Zomato failed", gpay))
        assertNull(PaymentTextParser.parse("Payment of ₹500 is pending", gpay))
    }

    @Test
    fun `ignores collect requests and promotions`() {
        assertNull(PaymentTextParser.parse("Rahul is requesting ₹300 from you", gpay))
        assertNull(PaymentTextParser.parse("Get cashback up to ₹100 when you pay with UPI", gpay))
    }

    @Test
    fun `ignores text without amount`() {
        assertNull(PaymentTextParser.parse("Payment successful", gpay))
    }
}
