package com.expenseassistant.categorize

import com.expenseassistant.data.local.MerchantRuleDao
import com.expenseassistant.data.model.Category
import com.expenseassistant.data.model.Direction
import com.expenseassistant.data.model.MerchantRule
import com.expenseassistant.parser.ParsedPayment

data class CategoryGuess(
    val category: Category,
    val confidence: Float,
    val merchantDisplayName: String? = null,
)

/**
 * Layered classifier:
 *  1. Rules the user taught the app (highest confidence).
 *  2. Built-in merchant/keyword knowledge base.
 *  3. Structural heuristics (UPI handle to a person, credits, amount bands).
 */
class Categorizer(private val merchantRuleDao: MerchantRuleDao) {

    suspend fun categorize(payment: ParsedPayment): CategoryGuess {
        val key = merchantKey(payment.merchantRaw)

        if (key != null) {
            merchantRuleDao.find(key)?.let {
                return CategoryGuess(it.category, 0.99f, it.displayName)
            }
        }

        if (payment.direction == Direction.CREDIT) {
            val fromKeywords = MerchantKeywords.match(payment.rawText)
            if (fromKeywords?.first == Category.INCOME) return CategoryGuess(Category.INCOME, 0.85f)
            return CategoryGuess(Category.INCOME, 0.55f)
        }

        MerchantKeywords.match(payment.merchantRaw.orEmpty())?.let { (category, len) ->
            return CategoryGuess(category, confidenceFor(len, exactField = true))
        }

        MerchantKeywords.match(payment.rawText)?.let { (category, len) ->
            return CategoryGuess(category, confidenceFor(len, exactField = false))
        }

        if (looksLikePersonHandle(payment.merchantRaw)) {
            return CategoryGuess(Category.TRANSFER, 0.6f)
        }

        return CategoryGuess(Category.OTHER, 0.2f)
    }

    /** Called when the user re-categorises a transaction so future ones match. */
    suspend fun learn(merchantRaw: String?, category: Category) {
        val key = merchantKey(merchantRaw) ?: return
        val existing = merchantRuleDao.find(key)
        merchantRuleDao.upsert(
            MerchantRule(
                merchantKey = key,
                category = category,
                displayName = existing?.displayName,
                hitCount = (existing?.hitCount ?: 0) + 1,
            )
        )
    }

    suspend fun learnDisplayName(merchantRaw: String?, displayName: String) {
        val key = merchantKey(merchantRaw) ?: return
        val existing = merchantRuleDao.find(key)
        merchantRuleDao.upsert(
            MerchantRule(
                merchantKey = key,
                category = existing?.category ?: Category.OTHER,
                displayName = displayName.trim().takeIf { it.isNotEmpty() },
                hitCount = (existing?.hitCount ?: 0) + 1,
            )
        )
    }

    suspend fun forgetAll() = merchantRuleDao.deleteAll()

    private fun confidenceFor(matchLength: Int, exactField: Boolean): Float {
        val base = if (exactField) 0.75f else 0.6f
        val lengthBonus = (matchLength.coerceAtMost(15) / 15f) * 0.2f
        return (base + lengthBonus).coerceAtMost(0.95f)
    }

    private fun looksLikePersonHandle(merchant: String?): Boolean {
        val value = merchant?.trim() ?: return false
        if (value.contains('@')) {
            val handle = value.substringBefore('@')
            // Personal VPAs are usually a phone number or a short personal name.
            return handle.all { it.isDigit() } || handle.count { it.isWhitespace() } == 0
        }
        val words = value.split(' ').filter { it.isNotBlank() }
        return words.size in 1..3 && words.all { word -> word.first().isUpperCase() && word.none { it.isDigit() } }
    }

    companion object {
        private val NON_ALNUM = Regex("[^a-z0-9]")
        private val TRAILING_NOISE = Regex(
            "(privatelimited|pvtltd|private|limited|ltd|llp|inc|india|payments?|solutions?|technologies|enterprises?)$"
        )

        fun merchantKey(merchantRaw: String?): String? {
            val base = merchantRaw?.substringBefore('@')?.lowercase()?.replace(NON_ALNUM, "") ?: return null
            val trimmed = TRAILING_NOISE.replace(base, "")
            val result = trimmed.ifEmpty { base }
            return result.takeIf { it.length >= 2 }
        }
    }
}
