package com.expenseassistant.parser

import com.expenseassistant.data.model.Direction

/**
 * Turns free-form payment text (notification body, SMS, or on-screen text) into a
 * [ParsedPayment]. Returns null when the text is not a completed money movement.
 */
object PaymentTextParser {

    private val AMOUNT = Regex(
        """(?:₹|rs\.?|inr)\s*([0-9]{1,3}(?:,[0-9]{2,3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)""",
        RegexOption.IGNORE_CASE,
    )

    private val DEBIT_HINTS = listOf(
        "paid", "sent", "debited", "spent", "you paid", "payment of", "purchase",
        "withdrawn", "transferred to", "successfully paid", "money sent",
    )

    private val CREDIT_HINTS = listOf(
        "received", "credited", "refund", "cashback", "added to", "money received",
        "has been credited", "you received",
    )

    private val SUCCESS_HINTS = listOf(
        "success", "successful", "completed", "paid", "sent", "debited", "credited",
        "received", "done", "transaction of",
    )

    private val FAILURE_HINTS = listOf(
        "failed", "failure", "declined", "cancelled", "canceled", "pending", "processing",
        "unsuccessful", "reversed", "will be", "request", "reminder", "expire", "due",
        "collect request", "requesting", "asked you", "offer", "cashback up to", "win ",
    )

    private val MERCHANT_PATTERNS = listOf(
        Regex("""(?:paid|sent)\s+(?:₹|rs\.?|inr)?\s*[0-9,.]*\s*to\s+([^.,\n₹]{2,60})""", RegexOption.IGNORE_CASE),
        Regex("""\bto\s+([A-Za-z0-9&'@._\- ]{2,60}?)(?=\s+(?:on|via|from|using|ref|upi|utr|for|at)\b|[.,\n]|$)""", RegexOption.IGNORE_CASE),
        Regex("""\b(?:at|from)\s+([A-Za-z0-9&'@._\- ]{2,60}?)(?=\s+(?:on|via|using|ref|upi|utr)\b|[.,\n]|$)""", RegexOption.IGNORE_CASE),
        Regex("""\b(?:vpa|upi id)\s*:?\s*([A-Za-z0-9._\-]{2,40}@[A-Za-z]{2,20})""", RegexOption.IGNORE_CASE),
    )

    private val REFERENCE = Regex(
        """(?:upi\s*(?:ref|txn)?\s*(?:id|no\.?)?|utr|ref(?:erence)?\s*(?:no\.?|id)?|txn\s*id)\s*[:# ]\s*([A-Za-z0-9]{6,25})""",
        RegexOption.IGNORE_CASE,
    )

    private val NOISE_MERCHANT_TOKENS = setOf(
        "your", "you", "account", "a/c", "bank", "upi", "wallet", "the", "and", "balance",
    )

    fun parse(
        text: String,
        sourcePackage: String,
        occurredAt: Long = System.currentTimeMillis(),
    ): ParsedPayment? {
        val normalized = text.replace('\u00A0', ' ').replace(Regex("\\s+"), " ").trim()
        if (normalized.isEmpty()) return null

        val lower = normalized.lowercase()
        if (FAILURE_HINTS.any { lower.contains(it) }) return null
        if (SUCCESS_HINTS.none { lower.contains(it) }) return null

        val amountMinor = extractAmountMinor(normalized) ?: return null
        if (amountMinor <= 0) return null

        val direction = extractDirection(lower) ?: return null

        return ParsedPayment(
            amountMinor = amountMinor,
            currency = "INR",
            direction = direction,
            merchantRaw = extractMerchant(normalized),
            referenceId = REFERENCE.find(normalized)?.groupValues?.get(1),
            rawText = normalized,
            sourcePackage = sourcePackage,
            sourceApp = PaymentApps.displayName(sourcePackage),
            occurredAt = occurredAt,
        )
    }

    private fun extractAmountMinor(text: String): Long? {
        val raw = AMOUNT.find(text)?.groupValues?.get(1) ?: return null
        val cleaned = raw.replace(",", "")
        val value = cleaned.toBigDecimalOrNull() ?: return null
        return value.movePointRight(2).toLong()
    }

    private fun extractDirection(lowerText: String): Direction? {
        val creditAt = CREDIT_HINTS.mapNotNull { hint -> lowerText.indexOf(hint).takeIf { it >= 0 } }.minOrNull()
        val debitAt = DEBIT_HINTS.mapNotNull { hint -> lowerText.indexOf(hint).takeIf { it >= 0 } }.minOrNull()
        return when {
            debitAt != null && (creditAt == null || debitAt <= creditAt) -> Direction.DEBIT
            creditAt != null -> Direction.CREDIT
            else -> null
        }
    }

    private fun extractMerchant(text: String): String? {
        for (pattern in MERCHANT_PATTERNS) {
            val candidate = pattern.find(text)?.groupValues?.get(1)?.trim()?.trim('-', '.', ',') ?: continue
            if (isPlausibleMerchant(candidate)) return candidate
        }
        return null
    }

    private fun isPlausibleMerchant(candidate: String): Boolean {
        if (candidate.length < 2 || candidate.length > 60) return false
        if (candidate.none { it.isLetter() }) return false
        val words = candidate.lowercase().split(' ').filter { it.isNotBlank() }
        if (words.isEmpty() || words.size > 8) return false
        return words.any { it !in NOISE_MERCHANT_TOKENS }
    }
}
