package com.khaltech.expenseassistant.parser

import com.khaltech.expenseassistant.data.model.AccountType
import com.khaltech.expenseassistant.data.model.PaymentMode
import com.khaltech.expenseassistant.data.model.TransactionType

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

    /** A bank alert states its rail and account, which beats guessing from wording. */
    fun detect(payment: ParsedPayment): PaymentMode = when {
        // RuPay credit cards pay over UPI, so the rail wins over the account type.
        payment.transactionType == TransactionType.UPI -> PaymentMode.UPI
        payment.accountType == AccountType.CREDIT_CARD || payment.accountType == AccountType.DEBIT_CARD -> PaymentMode.CARD
        payment.transactionType == TransactionType.CARD_POS || payment.transactionType == TransactionType.CARD_ONLINE -> PaymentMode.CARD
        payment.transactionType != null -> PaymentMode.BANK_ACCOUNT
        else -> detect(payment.rawText, payment.sourcePackage)
    }

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
