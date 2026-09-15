package com.khaltech.expenseassistant.parser.bank

import com.khaltech.expenseassistant.data.model.AccountType
import com.khaltech.expenseassistant.data.model.Direction
import com.khaltech.expenseassistant.data.model.TransactionType

data class BankTransaction(
    val amountMinor: Long,
    val direction: Direction,
    val type: TransactionType,
    val bank: Bank?,
    val accountType: AccountType?,
    val accountLast4: String?,
    val counterparty: String?,
    val referenceId: String?,
    /** Balance, or available limit on a credit card, as the alert reported it after this transaction. */
    val availableBalanceMinor: Long?,
)

sealed interface BankSmsResult {
    data class Transaction(val transaction: BankTransaction) : BankSmsResult

    /** A bank alert that is not a completed money movement: OTP, reminder, failure, promotion. */
    data object NotATransaction : BankSmsResult

    /** Not a bank alert at all, so a more general parser may still make sense of it. */
    data object Unrecognised : BankSmsResult
}

/**
 * Reads transaction alerts from Indian banks and card issuers. Works on structure rather than one
 * template per bank: banks change wording often, but every alert still names an amount, a
 * direction verb, an account or card, and usually a rail (UPI, NEFT, ATM...) and a counterparty.
 */
object BankSmsParser {

    /** Any character short of a full stop; the point inside "25,000.00" does not end a sentence. */
    private const val IN_SENTENCE = """(?:[^.]|\.(?=[0-9]))"""

    private const val AMOUNT_VALUE = """([0-9]+(?:,[0-9]{2,3})*(?:\.[0-9]{1,2})?)(?![0-9])"""

    private val AMOUNT = Regex("""(?<![A-Za-z])(?:₹|rs|inr)[.:]?\s*$AMOUNT_VALUE""", RegexOption.IGNORE_CASE)

    // SBI writes the amount with no currency at all: "A/C X2719 debited by 5000.00 on date 10Sep26".
    private val BARE_AMOUNT = Regex(
        """\b(?:debited|credited)\s+(?:by|for|with)\s+$AMOUNT_VALUE""",
        RegexOption.IGNORE_CASE,
    )

    /** An amount right after one of these is a balance or limit, not the transaction. */
    private val BALANCE_BEFORE = Regex("""(?:\bbal(?:ance)?|avlbal|avblbal|\blmt|\blimit|\boutstanding)\.?[^.;]{0,30}$""")

    private val ACCOUNT_NUMBER = Regex(
        """\b(?:account|acct|a/c|ac)(?:\s*(?:no|number)\.?)?[\s:.\-]*(?:ending\s*(?:with\s*)?)?([0-9x*#]*[0-9]{3,6})\b""",
        RegexOption.IGNORE_CASE,
    )
    private val CARD_NUMBER = Regex(
        """\b(?:card|cc|dc)(?:\s*(?:no|number|account|a/c|acct)\.?)?[\s:.\-]*(?:ending\s*(?:with|in)?\s*)?([0-9x*#]*[0-9]{4})\b""",
        RegexOption.IGNORE_CASE,
    )
    private val CREDIT_CARD_HINT = Regex(
        """credit\s*card|\bcc\b|card account|avl\.?\s*(?:credit\s*)?(?:limit|lmt)|available\s*(?:credit\s*)?limit"""
    )

    private val OTP = Regex(
        """\botp\b$IN_SENTENCE{0,40}\b(?:is|:)\s*[0-9]{4,8}\b|\b[0-9]{4,8}\s+is\s+(?:your|the)\s+(?:otp|one[- ]time)|one[- ]time password|verification code"""
    )
    private val NOT_A_TRANSACTION = Regex(
        listOf(
            // Future or conditional money movement
            """\b(?:will|shall|would|to|going to)\s+be\s+(?:auto[- ]?)?(?:debited|credited|deducted|charged|processed|presented|refunded|reversed)""",
            """\bwill\s+(?:reflect|get\s+(?:credited|debited))""",
            """\brefund$IN_SENTENCE{0,40}\binitiated\b""",
            """\b(?:pending|scheduled|upcoming|on hold)\b|\b(?:is|being|under)\s+processing\b|\bin process\b""",
            // Bills, statements and reminders
            """\b(?:is|are|falls?)\s+due\b|\bdue\s+(?:on|date|by)\b|\b(?:min(?:imum)?|total)\s*(?:amt\.?|amount)?\s*due\b|\boverdue\b""",
            """\breminder\b|\bstatement\b$IN_SENTENCE{0,40}\b(?:generated|is ready)\b""",
            // Requests the user has not paid yet
            """\bcollect request\b|\bpayment request\b|\bhas requested\b|\bis requesting\b|\brequested (?:money|payment)\b""",
            // Mandate setup is not a debit; the executions that follow are
            """\bmandate\b$IN_SENTENCE{0,60}\b(?:created|registered|set ?up|revoked|cancelled|paused|modified)\b""",
            // Marketing
            """\bpre[- ]?approved\b|\bapply now\b|\bcongratulations\b|\boffer\b|\bcashback up to\b|\bwin\b""",
        ).joinToString("|")
    )
    private val FAILED = Regex(
        """\b(?:failed|failure|declined|unsuccessful|rejected)\b|\bnot\s+(?:been\s+)?(?:processed|successful|completed)\b|\bcould not be\b|\binsufficient\s+(?:funds|balance|bal|limit)\b"""
    )

    private val DEBIT_VERB = Regex(
        """\b(?:debited|debit|dr|spent|withdrawn|withdrawal|wdl|paid|sent|transferred(?!\s+to\s+your)|trf|purchase|purchased|deducted|charged|used|money transfer)\b"""
    )
    private val CREDIT_VERB = Regex(
        """\b(?:credited|credit|cr|received|deposited|deposit|added|refunded|refund|reversed|reversal|reverted|cashback)\b"""
    )

    /** Card product names contain "debit"/"credit" but say nothing about which way money moved. */
    private val CARD_PRODUCT = Regex("""\b(?:debit|credit)\s*card\b|\bcredit\s*(?:limit|lmt)\b""")

    private val CARD_BILL_RECEIVED = Regex(
        """(?:payment|pymt|paymt)$IN_SENTENCE{0,60}\b(?:received|credited|realised|realized|posted)\b$IN_SENTENCE{0,60}(?:credit\s*card|card account|\bcc\b)|received\s+(?:a\s+)?payment$IN_SENTENCE{0,80}(?:credit\s*card|\bcc\b)"""
    )
    private val CARD_BILL_PAID = Regex(
        """\btowards\s+(?:your\s+)?(?:[a-z]+\s+){0,3}(?:credit\s*card|cc)\b|\b(?:credit\s*card|cc)\s+(?:bill\s+)?(?:payment|pymt|repayment|dues)\b|\bcard\s+bill\s+(?:payment|pymt)\b|\bbill\s*pay$IN_SENTENCE{0,20}(?:credit\s*card|\bcc\b)|\bcred\.club\b|\bcred club\b|\bto cred\b"""
    )
    private val REFUND = Regex(
        """\b(?:refund|refunded|reversal|reversed|reverted|chargeback)\b|\bcashback\s+(?:of|credited|received)\b|\bcredited$IN_SENTENCE{0,40}\bcashback\b"""
    )
    private val CHARGES = Regex(
        """\b(?:charges?|fees?|penalty|penal|gst|amc|annual maintenance|non[- ]maintenance|sms alert|late payment|finance charges?|bounce|return charges)\b"""
    )
    private val SALARY = Regex("""\b(?:salary|salaries|sal|payroll)\b""")
    private val INTEREST = Regex("""\binterest\b|\bint\.?\s*(?:pd|paid|cr|credited|credit)\b|\bsb\s*int\b""")
    private val CASH_DEPOSIT = Regex("""\bcash deposit|\bdeposited\b$IN_SENTENCE{0,20}\bcash\b|\bby cash\b|\bcdm\b""")
    private val ATM = Regex("""\batm\b|\bcash withdrawal\b|\bcash wdl\b|\bwithdrawn\b|\bwdl\b""")
    private val AUTO_DEBIT = Regex(
        """\bnach\b|\bach\b|\becs\b|\be-?mandate\b|\bmandate\b|\bauto[- ]?debit|\bautopay\b|\bauto pay\b|\bstanding instruction\b|\bsi\s+(?:txn|debit)\b|\bemi\s+(?:of|debited|deducted|amount|amt)\b|\bloan emi\b"""
    )
    private val RTGS = Regex("""\brtgs\b""")
    private val NEFT = Regex("""\bneft\b""")
    private val IMPS = Regex("""\bimps\b|\bmmid\b""")
    // HDFC names UPI only in its "SMS BLOCK UPI to ..." footer, which it sends on UPI alerts alone.
    private val UPI = Regex(
        """\bupi\b|\bvpa\b|\b[a-z0-9._-]{2,}@[a-z]{2,}\b(?!\w|\.[a-z])"""
    )
    private val CHEQUE = Regex("""\bcheque\b|\bchq\b|\bclg\b""")
    private val POS = Regex("""\bpos\b|\bswipe|\bcontactless\b|\btap\b""")
    // No URL checks: footers carry the bank's own site ("Report at sbicard.com") on every alert.
    private val ONLINE = Regex(
        """\bonline\b(?!\s+banking)|\be-?com(?:merce)?\b|\binternet\s+(?:txn|transaction|purchase)\b|\b(?:amazon|flipkart|myntra|ajio|meesho|nykaa|swiggy|zomato|netflix|spotify|hotstar|youtube|google|apple|microsoft|uber|ola|makemytrip|goibibo|irctc|bookmyshow|razorpay|payu|billdesk|cashfree|ccavenue|paypal|bigbasket|blinkit|zepto|airtel|jio)\b"""
    )

    // A counterparty name stops at the next keyword, punctuation, line break, or the " — " the
    // notification listener joins title and body with.
    private const val NAME = """([A-Za-z0-9][A-Za-z0-9&'@._\- ]{1,59}?)"""
    private const val END =
        """(?=\s+(?:on|via|from|using|ref|refno|rrn|upi|utr|txn|for|at|to|avl|avbl|bal|balance|not|if|info|with|by|dated|date|and|is|has|was|thru|through|imps|neft|rtgs|ending|value|call|sms|clear|total|—)\b|\s*[,;:()?!\n—]|\s*\.(?![A-Za-z0-9])|\s+-|$)"""

    private val PATH_NAME = Regex(
        """\b(?:upi|imps|neft|rtgs|mmt)[/-](?:(?:p2[amp]|dr|cr|[a-z]{2,6})[/-])?([A-Za-z0-9]*[0-9][A-Za-z0-9]{5,24})[/-]$NAME(?=[/-]|\s+(?:not you|if not|avl|bal)\b|${END.removePrefix("(?=")}""",
        RegexOption.IGNORE_CASE,
    )
    private val IFSC_NARRATION = Regex(
        """\b(?:neft|rtgs|imps)\s*(?:cr|dr)?[- ]+[A-Z]{4}0[A-Z0-9]{6}[- ]+([A-Za-z][A-Za-z0-9&'. ]{1,59}?)\s*-""",
        RegexOption.IGNORE_CASE,
    )
    private val MANDATE_NARRATION = Regex(
        """\b(?:nach|ach|ecs)\s*[-/]+(?:dr|debit|d)?[- /]*([A-Za-z][A-Za-z0-9&'. ]{1,59}?)\s*[-/]""",
        RegexOption.IGNORE_CASE,
    )
    /** Axis card alerts put the merchant on its own line under the timestamp. */
    private val LINE_AFTER_TIMESTAMP = Regex(
        """\b[0-9]{1,2}[-/][0-9]{1,2}[-/][0-9]{2,4},?\s+[0-9]{1,2}:[0-9]{2}(?::[0-9]{2})?(?:\s*IST)?\s*\n\s*([A-Za-z][^\n]{1,50}?)\s*(?:\n|$)""",
        RegexOption.IGNORE_CASE,
    )

    private val DEBIT_COUNTERPARTY = listOf(
        // ICICI: "...debited for Rs 55.00 on 03-Sep-26; BOTTLE LAB TECH credited."
        Regex("""[;,]\s*$NAME\s+credited\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(?:to|towards)\s+(?:vpa|beneficiary|a/c\s+of)?\s*$NAME$END""", RegexOption.IGNORE_CASE),
        Regex("""\bat\s+$NAME$END""", RegexOption.IGNORE_CASE),
        // ICICI card: "...on 12-Sep-26 on BLINK COMMERCE". The letter keeps the date from matching.
        Regex("""\bon\s+([A-Za-z][A-Za-z0-9&'@._\- ]{1,59}?)$END""", RegexOption.IGNORE_CASE),
    )
    private val CREDIT_COUNTERPARTY = listOf(
        Regex("""[;,]\s*$NAME\s+debited\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(?:from|by)\s+(?:vpa|remitter)?\s*$NAME$END""", RegexOption.IGNORE_CASE),
    )
    private val VPA = Regex("""\b([a-z0-9][a-z0-9._-]{1,50}@[a-z][a-z0-9]{1,20})\b(?!\w|\.[a-z])""", RegexOption.IGNORE_CASE)

    private val NOT_A_NAME_START = setOf(
        "your", "you", "a/c", "ac", "acct", "account", "card", "rs", "inr", "the", "mobile", "block", "report",
        "call", "sms", "forward", "fwd", "dispute", "avoid", "know", "check", "register", "avl", "available",
        "not", "date", "atm", "bank", "upi", "net", "cash", "self", "this", "customer", "dear", "ref",
    )
    private val NOISE_WORDS = setOf(
        "your", "you", "account", "a/c", "bank", "upi", "wallet", "the", "and", "balance", "imps", "neft",
        "rtgs", "transfer", "txn", "transaction", "payment",
    )

    private val REFERENCES = listOf(
        Regex("""\b(?:upi|imps|neft|rtgs)/(?:[A-Za-z0-9]+/)*?([0-9]{9,25})\b""", RegexOption.IGNORE_CASE),
        Regex(
            """\b(?:upi|imps|neft|rtgs)?\s*(?:ref(?:erence)?|txn|transaction)\s*(?:no|number|id|#)?\.?\s*[:#.\-]?\s*([A-Za-z0-9]{6,25})""",
            RegexOption.IGNORE_CASE,
        ),
        Regex("""\b(?:upi|imps|rrn|utr)\s*(?:no|number|ref)?\.?\s*[:#.\-]?\s*([A-Za-z0-9]{6,25})""", RegexOption.IGNORE_CASE),
        // NEFT/RTGS UTRs: sending bank's IFSC prefix, N or R, then the serial.
        Regex("""\b([A-Z]{4}[A-Z0-9]?[NR][0-9]{11,17})\b"""),
    )

    fun parse(body: String, sender: String? = null): BankSmsResult {
        val text = body.replace(' ', ' ')
            .lines().joinToString("\n") { it.replace(Regex("""[ \t]+"""), " ").trim() }
            .trim()
        if (text.isEmpty()) return BankSmsResult.Unrecognised
        if (Banks.isPromotionalSender(sender)) return BankSmsResult.NotATransaction

        val lower = text.lowercase()
        val bank = Banks.fromSender(sender) ?: Banks.fromText(text)
        val account = extractAccount(text, lower)
        val upi = UPI.containsMatchIn(lower)
        if (bank == null && account == null && !upi) return BankSmsResult.Unrecognised

        if (OTP.containsMatchIn(lower) || NOT_A_TRANSACTION.containsMatchIn(lower)) return BankSmsResult.NotATransaction

        val (amountMinor, balanceMinor) = extractAmounts(text) ?: return BankSmsResult.NotATransaction
        val (type, direction) = classify(lower, account?.first, upi) ?: return BankSmsResult.NotATransaction
        if (type != TransactionType.BANK_CHARGES && FAILED.containsMatchIn(lower)) return BankSmsResult.NotATransaction

        return BankSmsResult.Transaction(
            BankTransaction(
                amountMinor = amountMinor,
                direction = direction,
                type = type,
                bank = bank,
                accountType = account?.first,
                accountLast4 = account?.second,
                counterparty = extractCounterparty(text, direction, type),
                referenceId = extractReference(text),
                availableBalanceMinor = balanceMinor,
            )
        )
    }

    private fun classify(lower: String, accountType: AccountType?, upi: Boolean): Pair<TransactionType, Direction>? {
        val verbs = lower.replace(CARD_PRODUCT, " card ")
        val verb = verbDirection(verbs)
        fun has(pattern: Regex) = pattern.containsMatchIn(lower)
        val card = accountType == AccountType.DEBIT_CARD || accountType == AccountType.CREDIT_CARD

        return when {
            has(CARD_BILL_RECEIVED) -> TransactionType.CREDIT_CARD_BILL_PAYMENT to Direction.CREDIT
            verb == Direction.DEBIT && has(CARD_BILL_PAID) -> TransactionType.CREDIT_CARD_BILL_PAYMENT to Direction.DEBIT
            // A reversal names the original debit too, so it decides the direction on its own.
            has(REFUND) -> TransactionType.REFUND to Direction.CREDIT
            verb == null -> null
            verb == Direction.DEBIT && has(CHARGES) && !upi -> TransactionType.BANK_CHARGES to verb
            verb == Direction.CREDIT && has(SALARY) -> TransactionType.SALARY to verb
            verb == Direction.CREDIT && has(INTEREST) -> TransactionType.INTEREST to verb
            verb == Direction.CREDIT && has(CASH_DEPOSIT) -> TransactionType.CASH_DEPOSIT to verb
            verb == Direction.DEBIT && has(ATM) -> TransactionType.ATM_WITHDRAWAL to verb
            has(AUTO_DEBIT) -> TransactionType.AUTO_DEBIT to verb
            has(RTGS) -> TransactionType.RTGS to verb
            has(NEFT) -> TransactionType.NEFT to verb
            has(IMPS) -> TransactionType.IMPS to verb
            upi -> TransactionType.UPI to verb
            card || has(POS) -> (if (has(ONLINE)) TransactionType.CARD_ONLINE else TransactionType.CARD_POS) to verb
            has(CHEQUE) -> TransactionType.CHEQUE to verb
            else -> TransactionType.OTHER to verb
        }
    }

    /** The first direction verb in the message wins: "Dr. from A/C ... and Cr. to x@ybl" is a debit. */
    private fun verbDirection(lower: String): Direction? {
        val debitAt = DEBIT_VERB.find(lower)?.range?.first
        val creditAt = CREDIT_VERB.find(lower)?.range?.first
        return when {
            debitAt != null && (creditAt == null || debitAt < creditAt) -> Direction.DEBIT
            creditAt != null -> Direction.CREDIT
            else -> null
        }
    }

    /** @return the transaction amount and, when the alert states one, the balance after it. */
    private fun extractAmounts(text: String): Pair<Long, Long?>? {
        var transaction: Long? = null
        var balance: Long? = null
        for (match in AMOUNT.findAll(text)) {
            val value = toMinor(match.groupValues[1]) ?: continue
            val isBalance = BALANCE_BEFORE.containsMatchIn(text.substring(0, match.range.first).lowercase())
            if (isBalance) {
                if (balance == null) balance = value
            } else if (transaction == null) {
                transaction = value
            }
        }
        transaction = transaction ?: BARE_AMOUNT.find(text)?.groupValues?.get(1)?.let(::toMinor)
        return transaction?.takeIf { it > 0 }?.let { it to balance }
    }

    private fun toMinor(raw: String): Long? =
        raw.replace(",", "").toBigDecimalOrNull()?.movePointRight(2)?.toLong()

    private fun extractAccount(text: String, lower: String): Pair<AccountType, String?>? {
        val isCreditCard = CREDIT_CARD_HINT.containsMatchIn(lower)
        val card = CARD_NUMBER.find(text)?.groupValues?.get(1)?.let(::lastDigits)
        val account = ACCOUNT_NUMBER.find(text)?.groupValues?.get(1)?.let(::lastDigits)
        return when {
            card != null -> (if (isCreditCard) AccountType.CREDIT_CARD else AccountType.DEBIT_CARD) to card
            account != null -> AccountType.BANK_ACCOUNT to account
            isCreditCard -> AccountType.CREDIT_CARD to null
            else -> null
        }
    }

    private fun lastDigits(masked: String): String? =
        masked.takeLastWhile { it.isDigit() }.takeLast(4).takeIf { it.length >= 3 }

    private fun extractCounterparty(text: String, direction: Direction, type: TransactionType): String? {
        if (type == TransactionType.ATM_WITHDRAWAL || type == TransactionType.CASH_DEPOSIT) return null

        PATH_NAME.find(text)?.groupValues?.get(2)?.let(::cleanName)?.let { return it }
        IFSC_NARRATION.find(text)?.groupValues?.get(1)?.let(::cleanName)?.let { return it }
        if (type == TransactionType.AUTO_DEBIT) {
            MANDATE_NARRATION.find(text)?.groupValues?.get(1)?.let(::cleanName)?.let { return it }
        }
        LINE_AFTER_TIMESTAMP.find(text)?.groupValues?.get(1)?.let(::cleanName)?.let { return it }

        val patterns = if (direction == Direction.DEBIT) DEBIT_COUNTERPARTY else CREDIT_COUNTERPARTY
        for (pattern in patterns) {
            for (match in pattern.findAll(text)) {
                cleanName(match.groupValues[1])?.let { return it }
            }
        }
        return VPA.find(text)?.groupValues?.get(1)
    }

    private fun cleanName(candidate: String): String? {
        val name = candidate.trim().trim('-', '.', ',', '/', ' ')
        if (name.length < 2 || name.length > 60) return null
        if (name.none { it.isLetter() } || name.first().isDigit()) return null
        val words = name.lowercase().split(' ').filter { it.isNotBlank() }
        if (words.isEmpty() || words.size > 8) return null
        if (words.first() in NOT_A_NAME_START) return null
        if (words.any { it == "a/c" || it == "acct" || it == "account" }) return null
        if (words.all { it in NOISE_WORDS }) return null
        return name
    }

    private fun extractReference(text: String): String? = REFERENCES.firstNotNullOfOrNull { pattern ->
        pattern.findAll(text).map { it.groupValues[1] }.firstOrNull { ref -> ref.count { it.isDigit() } >= 6 }
    }
}
