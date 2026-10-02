package com.khaltech.expenseassistant.parser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentAppsTest {

    @Test
    fun `ignores whatsapp so chat notifications are never captured`() {
        assertFalse(PaymentApps.isSupported("com.whatsapp"))
    }

    @Test
    fun `still listens to upi apps and sms`() {
        assertTrue(PaymentApps.isSupported("com.google.android.apps.nbu.paisa.user"))
        assertTrue(PaymentApps.isSupported("com.google.android.apps.messaging"))
    }
}
