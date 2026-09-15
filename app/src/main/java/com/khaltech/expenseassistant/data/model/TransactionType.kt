package com.khaltech.expenseassistant.data.model

/** The rail or reason a bank alert names. Null on transactions captured before bank parsing existed. */
enum class TransactionType(val displayName: String) {
    UPI("UPI"),
    ATM_WITHDRAWAL("ATM withdrawal"),
    CARD_POS("Card swipe"),
    CARD_ONLINE("Online card payment"),
    NEFT("NEFT"),
    IMPS("IMPS"),
    RTGS("RTGS"),
    AUTO_DEBIT("Auto-debit (NACH/ECS/mandate)"),
    SALARY("Salary"),
    INTEREST("Interest"),
    BANK_CHARGES("Bank charges"),
    REFUND("Refund / reversal"),
    CASH_DEPOSIT("Cash deposit"),
    CHEQUE("Cheque"),

    /** Moves money the individual card spends already counted, so it is never recorded. */
    CREDIT_CARD_BILL_PAYMENT("Credit card bill payment"),
    OTHER("Bank transaction");

    companion object {
        fun fromName(value: String?): TransactionType? = entries.firstOrNull { it.name == value }
    }
}

enum class AccountType(val displayName: String) {
    BANK_ACCOUNT("A/c"),
    DEBIT_CARD("Debit card"),
    CREDIT_CARD("Credit card");

    companion object {
        fun fromName(value: String?): AccountType? = entries.firstOrNull { it.name == value }
    }
}
