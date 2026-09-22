package com.khaltech.expenseassistant.data.repo

import com.khaltech.expenseassistant.data.model.AccountType
import com.khaltech.expenseassistant.data.model.CaptureSource
import com.khaltech.expenseassistant.data.model.Category
import com.khaltech.expenseassistant.data.model.Direction
import com.khaltech.expenseassistant.data.model.TransactionEntity
import com.khaltech.expenseassistant.parser.ParsedPayment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureDedupeTest {

    @Test
    fun `the UPI app's copy and the bank alert for one payment are the same payment`() {
        val appCopy = stored()

        assertFalse(CaptureDedupe.isDistinct(appCopy, alert(bank = "HDFC", last4 = "1111")))
    }

    @Test
    fun `alerts from two accounts a minute apart are two payments`() {
        val first = stored(bank = "HDFC", last4 = "1111")

        assertTrue(CaptureDedupe.isDistinct(first, alert(bank = "HDFC", last4 = "2222")))
        assertTrue(CaptureDedupe.isDistinct(first, alert(bank = "SBI", last4 = null)))
    }

    @Test
    fun `different references are two payments`() {
        assertTrue(CaptureDedupe.isDistinct(stored(reference = "412345678901"), alert(reference = "498765432109")))
    }

    @Test
    fun `the same alert seen twice is one payment`() {
        assertFalse(CaptureDedupe.isDistinct(stored(bank = "HDFC", last4 = "1111"), alert(bank = "hdfc", last4 = "1111")))
    }

    @Test
    fun `a bank alert's account is filled into the app's copy, so the next payment can be told apart`() {
        val appCopy = stored()
        val firstAlert = alert(bank = "HDFC", last4 = "1111", reference = "412345678901")

        val enriched = CaptureDedupe.withDetailsFrom(appCopy, firstAlert)

        assertEquals("HDFC", enriched.bankName)
        assertEquals("1111", enriched.accountLast4)
        assertEquals(AccountType.BANK_ACCOUNT, enriched.accountType)
        assertEquals("412345678901", enriched.referenceId)
        // The second account's ₹1 alert no longer matches the first payment.
        assertTrue(CaptureDedupe.isDistinct(enriched, alert(bank = "HDFC", last4 = "2222")))
    }

    @Test
    fun `details already stored are not overwritten`() {
        val first = stored(bank = "HDFC", last4 = "1111")

        assertEquals("1111", CaptureDedupe.withDetailsFrom(first, alert(bank = "HDFC", last4 = "9999")).accountLast4)
    }

    private fun stored(bank: String? = null, last4: String? = null, reference: String? = null) = TransactionEntity(
        amountMinor = 100,
        direction = Direction.DEBIT,
        merchantRaw = "Rahul",
        merchant = "Rahul",
        category = Category.TRANSFER,
        categoryConfidence = 0.6f,
        sourcePackage = "com.google.android.apps.nbu.paisa.user",
        sourceApp = "Google Pay",
        captureSource = CaptureSource.NOTIFICATION,
        rawText = "",
        referenceId = reference,
        occurredAt = 0L,
        dedupeKey = "stored",
        bankName = bank,
        accountLast4 = last4,
    )

    private fun alert(bank: String? = null, last4: String? = null, reference: String? = null) = ParsedPayment(
        amountMinor = 100,
        currency = "INR",
        direction = Direction.DEBIT,
        merchantRaw = "rahul@okaxis",
        referenceId = reference,
        rawText = "Rs 1 debited",
        sourcePackage = "com.google.android.apps.messaging",
        sourceApp = "Messages",
        occurredAt = 60_000L,
        bankName = bank,
        accountType = if (bank != null) AccountType.BANK_ACCOUNT else null,
        accountLast4 = last4,
    )
}
