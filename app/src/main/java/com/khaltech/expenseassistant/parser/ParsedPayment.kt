package com.khaltech.expenseassistant.parser

import com.khaltech.expenseassistant.data.model.AccountType
import com.khaltech.expenseassistant.data.model.Direction
import com.khaltech.expenseassistant.data.model.TransactionType

data class ParsedPayment(
    val amountMinor: Long,
    val currency: String,
    val direction: Direction,
    val merchantRaw: String?,
    val referenceId: String?,
    val rawText: String,
    val sourcePackage: String,
    val sourceApp: String,
    val occurredAt: Long,
    // Filled in only for bank alerts; app notifications and payment screens do not state them.
    val bankName: String? = null,
    val accountType: AccountType? = null,
    val accountLast4: String? = null,
    val transactionType: TransactionType? = null,
    val availableBalanceMinor: Long? = null,
)
