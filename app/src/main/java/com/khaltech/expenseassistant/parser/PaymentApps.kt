package com.khaltech.expenseassistant.parser

/**
 * Packages we listen to. Anything not listed here is ignored outright so we never
 * touch unrelated notifications.
 */
object PaymentApps {

    private val known = mapOf(
        "com.google.android.apps.nbu.paisa.user" to "Google Pay",
        "com.phonepe.app" to "PhonePe",
        "com.phonepe.app.preprod" to "PhonePe",
        "net.one97.paytm" to "Paytm",
        "in.org.npci.upiapp" to "BHIM",
        "com.dreamplug.androidapp" to "CRED",
        "com.amazon.mShop.android.shopping" to "Amazon Pay",
        // WhatsApp is deliberately absent: its notifications are mostly personal chats, which read as
        // payments ("Sent a sticker", "I paid Rs 500"). WhatsApp Pay is UPI, so the bank SMS still records it.
        "com.mobikwik_new" to "MobiKwik",
        "com.freecharge.android" to "Freecharge",
        // Bank apps / SMS handlers
        "com.google.android.apps.messaging" to "SMS",
        "com.samsung.android.messaging" to "SMS",
        "com.android.mms" to "SMS",
        "com.truecaller" to "SMS",
        "com.sbi.lotusintouch" to "SBI",
        "com.snapwork.hdfc" to "HDFC Bank",
        "com.csam.icici.bank.imobile" to "ICICI Bank",
        "com.axis.mobile" to "Axis Bank",
        "com.msf.kbank.mobile" to "Kotak",
    )

    /** SMS apps and bank apps: their notifications carry bank alerts rather than UPI-app wording. */
    private val bankAlertPackages = setOf(
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging",
        "com.android.mms",
        "com.truecaller",
        "com.sbi.lotusintouch",
        "com.snapwork.hdfc",
        "com.csam.icici.bank.imobile",
        "com.axis.mobile",
        "com.msf.kbank.mobile",
    )

    fun isSupported(packageName: String?): Boolean = packageName != null && known.containsKey(packageName)

    fun carriesBankAlerts(packageName: String?): Boolean = packageName in bankAlertPackages

    fun displayName(packageName: String?): String =
        known[packageName] ?: packageName?.substringAfterLast('.')?.replaceFirstChar { it.uppercase() } ?: "Unknown"
}
