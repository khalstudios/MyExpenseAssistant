package com.khaltech.expenseassistant.data.repo

import com.khaltech.expenseassistant.data.model.Category
import com.khaltech.expenseassistant.data.model.Direction
import com.khaltech.expenseassistant.data.model.TransactionEntity

/**
 * A name the user has paid or been paid by before, carrying how its latest transaction was filed so
 * picking it can fill the category and tags in one go.
 */
data class MerchantSuggestion(
    val name: String,
    val direction: Direction,
    val useCount: Int,
    val lastUsedAt: Long,
    val category: Category,
    val customCategoryName: String?,
    val customCategoryColor: String?,
    val customCategoryIcon: String?,
    val tags: List<String>,
)

/** A note the user has written before. */
data class NoteSuggestion(val text: String, val useCount: Int, val lastUsedAt: Long)

/** Every merchant name in [transactions], most used first; names differing only in case are one. */
fun merchantSuggestions(transactions: List<TransactionEntity>): List<MerchantSuggestion> =
    transactions
        .filter { it.merchant.isNotBlank() }
        .groupBy { it.merchant.trim().lowercase() to it.direction }
        .map { (_, uses) ->
            val latest = uses.maxBy { it.occurredAt }
            MerchantSuggestion(
                name = latest.merchant.trim(),
                direction = latest.direction,
                useCount = uses.size,
                lastUsedAt = latest.occurredAt,
                category = latest.category,
                customCategoryName = latest.customCategoryName,
                customCategoryColor = latest.customCategoryColor,
                customCategoryIcon = latest.customCategoryIcon,
                tags = latest.tags,
            )
        }
        .sortedWith(compareByDescending<MerchantSuggestion> { it.useCount }.thenByDescending { it.lastUsedAt })

/** Every note in [transactions], most used first; notes differing only in case are one. */
fun noteSuggestions(transactions: List<TransactionEntity>): List<NoteSuggestion> =
    transactions
        .mapNotNull { tx -> tx.description?.trim()?.takeIf { it.isNotEmpty() }?.let { it to tx.occurredAt } }
        .groupBy { (text, _) -> text.lowercase() }
        .map { (_, uses) ->
            val latest = uses.maxBy { it.second }
            NoteSuggestion(text = latest.first, useCount = uses.size, lastUsedAt = latest.second)
        }
        .sortedWith(compareByDescending<NoteSuggestion> { it.useCount }.thenByDescending { it.lastUsedAt })

/**
 * The single most used option matching what has been typed: a word in it starts with the text, so
 * "insta" finds "Swiggy Instamart". Blank text offers the most used option outright. An option the
 * field already holds is never offered back.
 */
fun <T> bestSuggestion(typed: String, ranked: List<T>, textOf: (T) -> String): T? {
    val query = typed.trim().lowercase()
    return ranked.firstOrNull { option ->
        val text = textOf(option).lowercase()
        text != query && (query.isEmpty() || text.startsWith(query) || text.contains(" $query"))
    }
}

/** [current] with any of [added] it lacks appended; tags differing only in case count as the same. */
fun mergeTags(current: List<String>, added: List<String>): List<String> =
    current + added.filter { tag -> current.none { it.equals(tag, ignoreCase = true) } }
        .distinctBy { it.lowercase() }
