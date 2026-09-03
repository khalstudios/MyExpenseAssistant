package com.expenseassistant.parser

import com.expenseassistant.data.model.Direction
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Turns free-form payment text (notification body, SMS, or on-screen text) into a
 * [ParsedPayment]. Returns null when the text is not a completed money movement.
 */
object PaymentTextParser {

    // Unified digit run first, so a plain 4+ digit amount like "2300" is never
    // truncated by an alternative that only expected comma-grouped digits.
    private val AMOUNT = Regex(
        """(?:₹|rs\.?|inr)\s*([0-9]+(?:,[0-9]{2,3})*(?:\.[0-9]{1,2})?)""",
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

    // Screen text has no punctuation, so every pattern must stop at a trailing keyword.
    private const val MERCHANT_END = """(?=\s+(?:on|via|from|using|ref|upi|utr|txn|for|at|success|successful|completed|to)\b|[.,\n]|$)"""

    private val MERCHANT_PATTERNS = listOf(
        Regex("""(?:paid|sent)\s+(?:₹|rs\.?|inr)?\s*[0-9,.]*\s*to\s+([A-Za-z0-9&'@._\- ]{2,60}?)$MERCHANT_END""", RegexOption.IGNORE_CASE),
        // ICICI-style debits name the payee after the amount: "...debited for Rs 55 on 03-Sep-26; ACME credited".
        Regex("""[;,]\s*([A-Za-z0-9&'@._\- ]{2,60}?)\s+credited\b""", RegexOption.IGNORE_CASE),
        Regex("""\bto\s+([A-Za-z0-9&'@._\- ]{2,60}?)$MERCHANT_END""", RegexOption.IGNORE_CASE),
        Regex("""\b(?:at|from)\s+([A-Za-z0-9&'@._\- ]{2,60}?)$MERCHANT_END""", RegexOption.IGNORE_CASE),
        Regex("""\b(?:vpa|upi id)\s*:?\s*([A-Za-z0-9._\-]{2,40}@[A-Za-z]{2,20})""", RegexOption.IGNORE_CASE),
    )

    private val REFERENCE = Regex(
        """(?:upi\s*(?:ref|txn)?\s*(?:id|no\.?)?|utr|ref(?:erence)?\s*(?:no\.?|id)?|txn\s*id)\s*[:# ]\s*([A-Za-z0-9]{6,25})""",
        RegexOption.IGNORE_CASE,
    )

    private val NOISE_MERCHANT_TOKENS = setOf(
        "your", "you", "account", "a/c", "bank", "upi", "wallet", "the", "and", "balance",
    )

    /** Only a completed payment screen says this; history rows never do. */
    private val STRONG_SUCCESS = Regex(
        """\b(payment|transaction|transfer|money)?\s*(is\s+)?success(ful|fully)?\b""",
        RegexOption.IGNORE_CASE,
    )

    /** Wording that only appears on list, history or search screens. */
    private val HISTORY_HINTS = listOf(
        "transaction history", "all transactions", "payment history", "passbook",
        "statement", "view all", "search transactions", "recent transactions",
        "this month", "last month", "filter", "yesterday",
    )

    private val DATE_ISO = Regex("""\b(\d{4})-(\d{2})-(\d{2})\b""")
    private val DATE_NUMERIC = Regex("""\b(\d{1,2})[-/.](\d{1,2})[-/.](\d{2,4})\b""")
    private val DATE_TEXTUAL = Regex(
        """\b(\d{1,2})\s*(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*\.?\s*(\d{2,4})?\b""",
        RegexOption.IGNORE_CASE,
    )
    private val TIME_OF_DAY = Regex("""\b(\d{1,2}):(\d{2})\s*(am|pm)?\b""", RegexOption.IGNORE_CASE)

    private val MONTHS = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")

    /** How many separate amounts a genuine confirmation may show. */
    const val MAX_AMOUNTS_ON_CONFIRMATION = 2

    fun amountCount(text: String): Int = AMOUNT.findAll(text).count()

    fun looksLikeHistoryScreen(text: String): Boolean {
        val lower = text.lowercase()
        return HISTORY_HINTS.any { lower.contains(it) } ||
            amountCount(text) > MAX_AMOUNTS_ON_CONFIRMATION
    }

    /**
     * @param requireStrongSuccess rejects anything that is not an explicit "payment successful"
     * confirmation. Used for screen capture, where a scrolled history list would otherwise
     * look like dozens of live payments.
     */
    fun parse(
        text: String,
        sourcePackage: String,
        occurredAt: Long = System.currentTimeMillis(),
        requireStrongSuccess: Boolean = false,
    ): ParsedPayment? {
        val normalized = text.replace('\u00A0', ' ').replace(Regex("\\s+"), " ").trim()
        if (normalized.isEmpty()) return null

        val lower = normalized.lowercase()
        if (FAILURE_HINTS.any { lower.contains(it) }) return null
        if (SUCCESS_HINTS.none { lower.contains(it) }) return null

        if (requireStrongSuccess) {
            if (!STRONG_SUCCESS.containsMatchIn(normalized)) return null
            if (looksLikeHistoryScreen(normalized)) return null
        }

        val amountMinor = extractAmountMinor(normalized) ?: return null
        if (amountMinor <= 0) return null

        val direction = extractDirection(lower) ?: return null
        val merchant = extractMerchant(normalized)

        return ParsedPayment(
            amountMinor = amountMinor,
            currency = "INR",
            direction = direction,
            merchantRaw = merchant,
            referenceId = REFERENCE.find(normalized)?.groupValues?.get(1),
            rawText = normalized,
            sourcePackage = sourcePackage,
            sourceApp = PaymentApps.displayName(sourcePackage),
            occurredAt = resolveOccurredAt(normalized, occurredAt),
        )
    }

    /**
     * Prefers a date written in the text, but only when it is clearly older than the
     * capture time — otherwise the arrival timestamp keeps its time-of-day precision.
     */
    private fun resolveOccurredAt(text: String, fallback: Long): Long {
        val parsed = extractDate(text) ?: return fallback
        val now = System.currentTimeMillis()
        if (parsed > now) return fallback
        if (fallback - parsed < TimeUnit.DAYS.toMillis(1)) return fallback
        if (now - parsed > TimeUnit.DAYS.toMillis(400)) return fallback
        return parsed
    }

    private fun extractDate(text: String): Long? {
        val calendar = Calendar.getInstance()
        val thisYear = calendar.get(Calendar.YEAR)

        val (day, month, year) = when {
            DATE_ISO.containsMatchIn(text) -> DATE_ISO.find(text)!!.groupValues.let {
                Triple(it[3].toInt(), it[2].toInt() - 1, it[1].toInt())
            }

            DATE_NUMERIC.containsMatchIn(text) -> DATE_NUMERIC.find(text)!!.groupValues.let {
                val rawYear = it[3].toInt()
                Triple(it[1].toInt(), it[2].toInt() - 1, if (rawYear < 100) 2000 + rawYear else rawYear)
            }

            DATE_TEXTUAL.containsMatchIn(text) -> DATE_TEXTUAL.find(text)!!.groupValues.let {
                val rawYear = it[3].toIntOrNull()
                Triple(
                    it[1].toInt(),
                    MONTHS.indexOf(it[2].lowercase()),
                    when {
                        rawYear == null -> thisYear
                        rawYear < 100 -> 2000 + rawYear
                        else -> rawYear
                    },
                )
            }

            else -> return null
        }

        if (month !in 0..11 || day !in 1..31) return null

        val time = TIME_OF_DAY.find(text)?.groupValues
        val hour = time?.get(1)?.toIntOrNull() ?: 0
        val minute = time?.get(2)?.toIntOrNull() ?: 0
        val isPm = time?.get(3)?.lowercase() == "pm"

        return calendar.apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, if (isPm && hour < 12) hour + 12 else hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
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
