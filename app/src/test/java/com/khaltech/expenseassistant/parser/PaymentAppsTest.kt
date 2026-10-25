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

    @Test
    fun `sms apps are told apart from bank and upi apps`() {
        assertTrue(PaymentApps.isSmsApp("com.google.android.apps.messaging"))
        assertTrue(PaymentApps.isSmsApp("com.truecaller"))
        assertTrue(PaymentApps.carriesBankAlerts("com.samsung.android.messaging"))
        assertFalse(PaymentApps.isSmsApp("com.snapwork.hdfc"))
        assertTrue(PaymentApps.carriesBankAlerts("com.snapwork.hdfc"))
        assertFalse(PaymentApps.isSmsApp("com.phonepe.app"))
    }
}
