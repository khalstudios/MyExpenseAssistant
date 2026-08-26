package com.expenseassistant.parser

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
        "com.whatsapp" to "WhatsApp Pay",
        "com.mobikwik_new" to "MobiKwik",
        "com.freecharge.android" to "Freecharge",
        // Bank apps / SMS handlers
        "com.google.android.apps.messaging" to "SMS",
        "com.samsung.android.messaging" to "SMS",
        "com.android.mms" to "SMS",
        "com.sbi.lotusintouch" to "SBI",
        "com.snapwork.hdfc" to "HDFC Bank",
        "com.csam.icici.bank.imobile" to "ICICI Bank",
        "com.axis.mobile" to "Axis Bank",
        "com.msf.kbank.mobile" to "Kotak",
    )

    fun isSupported(packageName: String?): Boolean = packageName != null && known.containsKey(packageName)

    fun displayName(packageName: String?): String =
        known[packageName] ?: packageName?.substringAfterLast('.')?.replaceFirstChar { it.uppercase() } ?: "Unknown"

    /** Packages whose success screens the accessibility service should scan. */
    val screenScanTargets = setOf(
        "com.google.android.apps.nbu.paisa.user",
        "com.phonepe.app",
        "net.one97.paytm",
        "in.org.npci.upiapp",
    )
}
