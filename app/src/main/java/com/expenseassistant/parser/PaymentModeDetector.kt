package com.expenseassistant.parser

import com.expenseassistant.data.model.PaymentMode

object PaymentModeDetector {

    private val walletPackages = setOf(
        "com.mobikwik_new",
        "com.freecharge.android",
        "com.amazon.mShop.android.shopping",
    )

    private val upiPackages = setOf(
        "com.google.android.apps.nbu.paisa.user",
        "com.phonepe.app",
        "com.phonepe.app.preprod",
        "net.one97.paytm",
        "in.org.npci.upiapp",
        "com.whatsapp",
        "com.dreamplug.androidapp",
    )

    fun detect(rawText: String, sourcePackage: String?): PaymentMode {
        val text = rawText.lowercase()
        return when {
            text.contains("credit card") || text.contains("debit card") ||
                text.contains("card ending") || Regex("""\bcard\s*(no\.?|x+\d)""").containsMatchIn(text) -> PaymentMode.CARD

            text.contains("upi") || text.contains("vpa") || text.contains("@ok") ||
                sourcePackage in upiPackages -> PaymentMode.UPI

            text.contains("wallet") || sourcePackage in walletPackages -> PaymentMode.WALLET

            text.contains("a/c") || text.contains("account") || text.contains("net banking") ||
                text.contains("neft") || text.contains("imps") || text.contains("rtgs") -> PaymentMode.BANK_ACCOUNT

            else -> PaymentMode.UNKNOWN
        }
    }
}
