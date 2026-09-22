package com.khaltech.expenseassistant.data.repo

import com.khaltech.expenseassistant.data.model.TransactionEntity
import com.khaltech.expenseassistant.parser.ParsedPayment

/**
 * Deciding whether a capture is a second sighting of a payment already stored.
 *
 * One payment routinely arrives twice: from the UPI app, which states no account, and as a bank
 * alert, which does. So a capture matching a stored payment's amount and direction within a few
 * minutes is presumed to be the same one. That presumption only holds while nothing says
 * otherwise: two alerts naming different accounts, banks or references are two payments, however
 * close together, such as ₹1 sent from two accounts a minute apart.
 */
internal object CaptureDedupe {

    /** True when the two carry details that cannot both describe one payment. */
    fun isDistinct(stored: TransactionEntity, capture: ParsedPayment): Boolean =
        conflicts(stored.referenceId, capture.referenceId) ||
            conflicts(stored.accountLast4, capture.accountLast4) ||
            conflicts(stored.bankName, capture.bankName)

    /**
     * The stored payment with whatever the duplicate stated that it did not: typically the bank
     * and account from a bank alert, filled into the UPI app's copy. Without this the next
     * payment's alert would meet a stored copy with no account to tell it apart by.
     */
    fun withDetailsFrom(stored: TransactionEntity, capture: ParsedPayment): TransactionEntity = stored.copy(
        referenceId = stored.referenceId ?: capture.referenceId,
        bankName = stored.bankName ?: capture.bankName,
        accountType = stored.accountType ?: capture.accountType,
        accountLast4 = stored.accountLast4 ?: capture.accountLast4,
        transactionType = stored.transactionType ?: capture.transactionType,
        availableBalanceMinor = stored.availableBalanceMinor ?: capture.availableBalanceMinor,
    )

    /** Only two stated values can conflict; a detail one side never mentioned proves nothing. */
    private fun conflicts(a: String?, b: String?): Boolean =
        !a.isNullOrBlank() && !b.isNullOrBlank() && !a.trim().equals(b.trim(), ignoreCase = true)
}
