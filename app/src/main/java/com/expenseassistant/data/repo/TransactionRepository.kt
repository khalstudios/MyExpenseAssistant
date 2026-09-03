package com.expenseassistant.data.repo

import com.expenseassistant.categorize.Categorizer
import com.expenseassistant.data.local.ContactNameCacheDao
import com.expenseassistant.data.local.TransactionDao
import com.expenseassistant.data.model.CaptureSource
import com.expenseassistant.data.model.Category
import com.expenseassistant.data.model.ContactNameCache
import com.expenseassistant.data.model.Direction
import com.expenseassistant.data.model.PaymentMode
import com.expenseassistant.data.model.TransactionEntity
import com.expenseassistant.notify.BudgetNotifier
import com.expenseassistant.parser.ParsedPayment
import com.expenseassistant.parser.PaymentModeDetector
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

data class TagUsage(val tag: String, val count: Int)

data class CustomCategoryOption(val name: String, val colorHex: String, val iconKey: String? = null)

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val categorizer: Categorizer,
    private val contactNameCacheDao: ContactNameCacheDao? = null,
    private val contactResolver: ContactResolver? = null,
    private val budgetNotifier: BudgetNotifier? = null,
) {

    fun observeAll(): Flow<List<TransactionEntity>> = transactionDao.observeAll()

    fun observeSince(from: Long): Flow<List<TransactionEntity>> = transactionDao.observeSince(from)

    fun observeBetween(from: Long, to: Long): Flow<List<TransactionEntity>> =
        transactionDao.observeBetween(from, to)

    fun observeCount(): Flow<Int> = transactionDao.observeCount()

    suspend fun allTransactions(): List<TransactionEntity> = transactionDao.allOnce()

    suspend fun transactionsForTag(tag: String): List<TransactionEntity> =
        allTransactions().filter { tx -> tx.tags.any { it.equals(tag, ignoreCase = true) } }

    /** Every tag in use, most-used first, for the "browse by tag" widget. */
    fun observeTagUsage(): Flow<List<TagUsage>> = transactionDao.observeAll().map { transactions ->
        val counts = LinkedHashMap<String, Int>()
        transactions.forEach { tx ->
            tx.tags.forEach { tag ->
                val key = tag.trim()
                if (key.isNotEmpty()) counts[key] = (counts[key] ?: 0) + 1
            }
        }
        counts.entries.sortedByDescending { it.value }.map { TagUsage(it.key, it.value) }
    }

    /** Tags ordered by how often they're used, so recent/common ones surface first as suggestions. */
    fun observeTagSuggestions(): Flow<List<String>> = observeTagUsage().map { usages -> usages.map { it.tag } }

    /** Custom categories the user has created before, most recently used first, for reuse in the picker. */
    fun observeCustomCategorySuggestions(): Flow<List<CustomCategoryOption>> = transactionDao.observeAll().map { transactions ->
        val seen = LinkedHashMap<String, CustomCategoryOption>()
        transactions.sortedByDescending { it.occurredAt }.forEach { tx ->
            val name = tx.customCategoryName?.trim()
            val colorHex = tx.customCategoryColor
            if (!name.isNullOrEmpty() && colorHex != null && !seen.containsKey(name.lowercase())) {
                seen[name.lowercase()] = CustomCategoryOption(name, colorHex, tx.customCategoryIcon)
            }
        }
        seen.values.toList()
    }

    suspend fun earliestTimestamp(): Long? = transactionDao.earliestTimestamp()

    suspend fun deleteAll() = transactionDao.deleteAll()

    suspend fun clearLearnedRules() {
        categorizer.forgetAll()
        contactNameCacheDao?.deleteAll()
    }

    /** Adds a transaction the capture services could not see, such as cash or a card swipe. */
    suspend fun addManual(
        amountMinor: Long,
        direction: Direction,
        merchant: String,
        category: Category,
        customCategoryName: String? = null,
        customCategoryColor: String? = null,
        customCategoryIcon: String? = null,
        paymentMode: PaymentMode,
        occurredAt: Long,
        description: String?,
        tags: List<String>,
    ): Long {
        val entity = TransactionEntity(
            amountMinor = amountMinor,
            direction = direction,
            merchantRaw = merchant,
            merchant = merchant,
            category = category,
            categoryConfidence = 1f,
            sourcePackage = null,
            sourceApp = "Added manually",
            captureSource = CaptureSource.MANUAL,
            rawText = description.orEmpty(),
            referenceId = null,
            occurredAt = occurredAt,
            description = description?.takeIf { it.isNotBlank() },
            tags = tags,
            paymentMode = paymentMode,
            userCorrected = true,
            dedupeKey = "manual:${UUID.randomUUID()}",
            customCategoryName = customCategoryName,
            customCategoryColor = customCategoryColor,
            customCategoryIcon = customCategoryIcon,
        )
        if (customCategoryName == null) categorizer.learn(merchant, category)
        return transactionDao.insert(entity).also { id ->
            budgetNotifier?.onTransactionRecorded(entity.copy(id = id))
        }
    }

    /** Stores every parsed payment notification or payment-success screen. */
    suspend fun ingest(payment: ParsedPayment, captureSource: CaptureSource): Long? {
        val guess = categorizer.categorize(payment)
        val originalMerchant = payment.merchantRaw?.trim()?.takeIf { it.isNotEmpty() }
        val contactName = originalMerchant?.takeIf { guess.merchantDisplayName == null }?.let { merchant ->
            val key = Categorizer.merchantKey(merchant)
            val cached = key?.let { contactNameCacheDao?.find(it) }
            when {
                cached != null -> cached.contactName
                key != null && contactResolver?.hasAccess() == true -> {
                    contactResolver.resolve(merchant).also { name ->
                        contactNameCacheDao?.upsert(ContactNameCache(key, name))
                    }
                }
                else -> null
            }
        }
        val merchantName = guess.merchantDisplayName ?: contactName ?: originalMerchant ?: payment.sourceApp
        val entity = TransactionEntity(
            amountMinor = payment.amountMinor,
            currency = payment.currency,
            direction = payment.direction,
            merchantRaw = payment.merchantRaw,
            merchant = merchantName,
            category = guess.category,
            categoryConfidence = guess.confidence,
            sourcePackage = payment.sourcePackage,
            sourceApp = payment.sourceApp,
            captureSource = captureSource,
            rawText = payment.rawText,
            referenceId = payment.referenceId,
            occurredAt = payment.occurredAt,
            description = originalMerchant?.takeIf { merchantName != it },
            paymentMode = PaymentModeDetector.detect(payment.rawText, payment.sourcePackage),
            dedupeKey = "capture:${UUID.randomUUID()}",
        )
        return transactionDao.insert(entity).also { id ->
            budgetNotifier?.onTransactionRecorded(entity.copy(id = id))
        }
    }

    fun observeById(id: Long): Flow<TransactionEntity?> = transactionDao.observeById(id)

    suspend fun recategorize(id: Long, category: Category, customName: String? = null, customColorHex: String? = null, customIconKey: String? = null) {
        val existing = transactionDao.findById(id) ?: return
        transactionDao.update(
            existing.copy(
                category = category,
                categoryConfidence = 1f,
                userCorrected = true,
                customCategoryName = customName,
                customCategoryColor = customColorHex,
                customCategoryIcon = customIconKey,
            )
        )
        if (customName == null) {
            categorizer.learn(existing.merchantRaw ?: existing.merchant, category)
        }
    }

    suspend fun updateDescription(id: Long, description: String?) {
        val existing = transactionDao.findById(id) ?: return
        transactionDao.update(existing.copy(description = description?.takeIf { it.isNotBlank() }))
    }

    suspend fun updateTags(id: Long, tags: List<String>) {
        val existing = transactionDao.findById(id) ?: return
        val cleaned = tags.map { it.trim().removePrefix("#") }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
        transactionDao.update(existing.copy(tags = cleaned))
    }

    suspend fun updatePaymentMode(id: Long, mode: PaymentMode) {
        val existing = transactionDao.findById(id) ?: return
        transactionDao.update(existing.copy(paymentMode = mode))
    }

    /** Lets the user correct anything the capture pipeline got wrong, including manual entries. */
    suspend fun updateCore(
        id: Long,
        amountMinor: Long,
        direction: Direction,
        merchant: String,
        occurredAt: Long,
    ) {
        val existing = transactionDao.findById(id) ?: return
        transactionDao.update(
            existing.copy(
                amountMinor = amountMinor,
                direction = direction,
                merchant = merchant,
                merchantRaw = merchant,
                occurredAt = occurredAt,
                userCorrected = true,
            )
        )
    }

    /** Single commit for the detail screen, which batches every edit behind one save action. */
    suspend fun updateDetails(
        id: Long,
        amountMinor: Long,
        direction: Direction,
        merchant: String,
        occurredAt: Long,
        category: Category,
        customCategoryName: String?,
        customCategoryColor: String?,
        customCategoryIcon: String?,
        paymentMode: PaymentMode,
        description: String?,
        tags: List<String>,
    ) {
        val existing = transactionDao.findById(id) ?: return
        val categoryChanged = existing.category != category ||
            existing.customCategoryName != customCategoryName
        val cleanedTags = tags.map { it.trim().removePrefix("#") }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
        val trimmedMerchant = merchant.trim()
        val merchantRenamed = trimmedMerchant != existing.merchant
        transactionDao.update(
            existing.copy(
                amountMinor = amountMinor,
                direction = direction,
                merchant = trimmedMerchant,
                merchantRaw = trimmedMerchant,
                occurredAt = occurredAt,
                category = category,
                categoryConfidence = if (categoryChanged) 1f else existing.categoryConfidence,
                customCategoryName = customCategoryName,
                customCategoryColor = customCategoryColor,
                customCategoryIcon = customCategoryIcon,
                paymentMode = paymentMode,
                description = description?.takeIf { it.isNotBlank() },
                tags = cleanedTags,
                userCorrected = true,
            )
        )
        if (categoryChanged && customCategoryName == null) {
            categorizer.learn(existing.merchantRaw ?: existing.merchant, category)
        }
        if (merchantRenamed) {
            categorizer.learnDisplayName(existing.merchantRaw ?: existing.merchant, trimmedMerchant)
        }
    }

    suspend fun delete(id: Long) = transactionDao.delete(id)
}
