package com.expenseassistant.parser

import com.expenseassistant.data.model.Direction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

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

    @Test
    fun `screen capture rejects a scrolled history list`() {
        val history = "Transaction history Paid to Swiggy \u20b9249 Paid to Uber \u20b9180 Paid to Zepto \u20b9640"
        assertNull(PaymentTextParser.parse(history, gpay, requireStrongSuccess = true))
    }

    @Test
    fun `screen capture rejects a single history row without a success banner`() {
        val row = "Paid to Swiggy \u20b9249 12 Aug 2026"
        assertNull(PaymentTextParser.parse(row, gpay, requireStrongSuccess = true))
    }

    @Test
    fun `screen capture accepts a live confirmation screen`() {
        val screen = "Payment successful \u20b9249 Paid to Swiggy UPI Ref No 512345678901"
        val result = PaymentTextParser.parse(screen, gpay, requireStrongSuccess = true)
        assertNotNull(result)
        assertEquals(24900L, result!!.amountMinor)
        assertEquals("Swiggy", result.merchantRaw)
    }

    @Test
    fun `uses the date written in the text when it is clearly older`() {
        val capturedAt = System.currentTimeMillis()
        val text = "Rs.1,250.00 debited from A/c XX1234 to UBER INDIA on 12-05-2026"
        val result = PaymentTextParser.parse(text, gpay, occurredAt = capturedAt)
        assertNotNull(result)
        assertTrue(result!!.occurredAt < capturedAt)
    }

    @Test
    fun `keeps the capture time when the written date is today`() {
        val now = Calendar.getInstance()
        val today = "${now.get(Calendar.DAY_OF_MONTH)}-${now.get(Calendar.MONTH) + 1}-${now.get(Calendar.YEAR)}"
        val capturedAt = now.timeInMillis
        val result = PaymentTextParser.parse("You paid \u20b9100 to Swiggy on $today", gpay, capturedAt)
        assertNotNull(result)
        assertEquals(capturedAt, result!!.occurredAt)
    }

    @Test
    fun `flags screens showing many amounts as history`() {
        assertTrue(
            PaymentTextParser.looksLikeHistoryScreen("\u20b9100 \u20b9200 \u20b9300 \u20b9400")
        )
        assertFalse(
            PaymentTextParser.looksLikeHistoryScreen("Payment successful \u20b9249 Paid to Swiggy")
        )
    }
}
