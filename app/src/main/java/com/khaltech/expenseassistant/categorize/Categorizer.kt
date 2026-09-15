package com.khaltech.expenseassistant.categorize

import com.khaltech.expenseassistant.data.local.MerchantRuleDao
import com.khaltech.expenseassistant.data.model.Category
import com.khaltech.expenseassistant.data.model.Direction
import com.khaltech.expenseassistant.data.model.MerchantRule
import com.khaltech.expenseassistant.parser.ParsedPayment

data class CategoryGuess(
    val category: Category,
    val confidence: Float,
    val merchantDisplayName: String? = null,
    val tags: List<String> = emptyList(),
)

/**
 * Layered classifier:
 *  1. Rules the user taught the app (highest confidence). Spending only; income ignores them.
 *  2. Built-in merchant/keyword knowledge base.
 *  3. Structural heuristics (UPI handle to a person, credits, amount bands).
 */
class Categorizer(private val merchantRuleDao: MerchantRuleDao) {

    suspend fun categorize(payment: ParsedPayment): CategoryGuess {
        // Income never picks up merchant rules: money coming in from a merchant should not inherit
        // the category, name or tags the user taught for paying them.
        if (payment.direction == Direction.CREDIT) {
            val fromKeywords = MerchantKeywords.match(payment.rawText)
            if (fromKeywords?.first == Category.INCOME) return CategoryGuess(Category.INCOME, 0.85f)
            return CategoryGuess(Category.INCOME, 0.55f)
        }

        val rule = merchantKey(payment.merchantRaw)?.let { merchantRuleDao.find(it) }
        // A rule saved only for a name or tags carries OTHER because no category was ever taught.
        // It must not pin the merchant to Unknown, so the category comes from the layers below.
        if (rule != null && rule.category != Category.OTHER) {
            return CategoryGuess(rule.category, 0.99f, rule.displayName, rule.tags)
        }
        val guess = builtInGuess(payment)
        return if (rule == null) guess else guess.copy(merchantDisplayName = rule.displayName, tags = rule.tags)
    }

    private fun builtInGuess(payment: ParsedPayment): CategoryGuess {
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
    suspend fun learn(merchantRaw: String?, direction: Direction, category: Category) {
        val key = learnableKey(merchantRaw, direction) ?: return
        val existing = merchantRuleDao.find(key)
        merchantRuleDao.upsert(
            ruleFor(key, existing).copy(category = category)
        )
    }

    suspend fun learnDisplayName(merchantRaw: String?, direction: Direction, displayName: String) {
        val key = learnableKey(merchantRaw, direction) ?: return
        val existing = merchantRuleDao.find(key)
        merchantRuleDao.upsert(
            ruleFor(key, existing).copy(displayName = displayName.trim().takeIf { it.isNotEmpty() })
        )
    }

    /**
     * Remembers the tags the user last saved for a merchant so the next payment to the same place
     * arrives already carrying them. Saving with an empty list clears what was learned. Notes are
     * not learned: they describe one transaction, not the merchant.
     */
    suspend fun learnTags(merchantRaw: String?, direction: Direction, tags: List<String>) {
        val key = learnableKey(merchantRaw, direction) ?: return
        val existing = merchantRuleDao.find(key)
        merchantRuleDao.upsert(
            ruleFor(key, existing).copy(tags = tags.map { it.trim() }.filter { it.isNotEmpty() })
        )
    }

    /** Only spending teaches merchant rules, mirroring [categorize], which skips them for income. */
    private fun learnableKey(merchantRaw: String?, direction: Direction): String? =
        if (direction == Direction.CREDIT) null else merchantKey(merchantRaw)

    /** Carries every field of an existing rule forward so one lesson never erases another. */
    private fun ruleFor(key: String, existing: MerchantRule?) = MerchantRule(
        merchantKey = key,
        category = existing?.category ?: Category.OTHER,
        displayName = existing?.displayName,
        tags = existing?.tags.orEmpty(),
        // Notes are no longer learned; this drops any an earlier version stored on the rule.
        note = null,
        hitCount = (existing?.hitCount ?: 0) + 1,
    )

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
