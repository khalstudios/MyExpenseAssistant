package com.expenseassistant.data.model

enum class PaymentMode(val displayName: String) {
    UPI("UPI"),
    BANK_ACCOUNT("Bank account"),
    CARD("Card"),
    WALLET("Wallet"),
    CASH("Cash"),
    UNKNOWN("Not set");

    companion object {
        fun fromName(value: String?): PaymentMode =
            entries.firstOrNull { it.name == value } ?: UNKNOWN
    }
}
