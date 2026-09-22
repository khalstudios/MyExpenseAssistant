package com.khaltech.expenseassistant.data.repo

import android.util.Log
import com.khaltech.expenseassistant.categorize.Categorizer
import com.khaltech.expenseassistant.data.local.ContactNameCacheDao
import com.khaltech.expenseassistant.data.local.TransactionDao
import com.khaltech.expenseassistant.data.model.CaptureSource
import com.khaltech.expenseassistant.data.model.Category
import com.khaltech.expenseassistant.data.model.ContactNameCache
import com.khaltech.expenseassistant.data.model.Direction
import com.khaltech.expenseassistant.data.model.PaymentMode
import com.khaltech.expenseassistant.data.model.TransactionEntity
import com.khaltech.expenseassistant.notify.BudgetNotifier
import com.khaltech.expenseassistant.notify.TransactionNotifier
import com.khaltech.expenseassistant.parser.ParsedPayment
import com.khaltech.expenseassistant.parser.PaymentModeDetector
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit

data class TagUsage(val tag: String, val count: Int, val spentMinor: Long = 0L)

/**
 * Tags used by [transactions], most-used first. Kept free of the DAO so the insights screen can
 * aggregate the period it is showing rather than the whole history.
 */
fun tagUsageOf(transactions: List<TransactionEntity>): List<TagUsage> {
    val counts = LinkedHashMap<String, Int>()
    val spend = LinkedHashMap<String, Long>()
    transactions.forEach { tx ->
        tx.tags.forEach { tag ->
            val key = tag.trim()
            if (key.isNotEmpty()) {
                counts[key] = (counts[key] ?: 0) + 1
                if (tx.direction == Direction.DEBIT) {
                    spend[key] = (spend[key] ?: 0L) + tx.amountMinor
                }
            }
        }
    }
    return counts.entries.sortedByDescending { it.value }
        .map { TagUsage(it.key, it.value, spend[it.key] ?: 0L) }
}

/**
 * Transactions carrying [tag] within an optional window; a null bound is unbounded, so the insights
 * screen can ask for just the period it shows while the detail screen still opens the full history.
 */
fun List<TransactionEntity>.taggedWith(
    tag: String,
    from: Long? = null,
    toExclusive: Long? = null,
): List<TransactionEntity> = filter { tx ->
    tx.tags.any { it.equals(tag, ignoreCase = true) } &&
        (from == null || tx.occurredAt >= from) &&
        (toExclusive == null || tx.occurredAt < toExclusive)
}

data class CustomCategoryOption(
    val name: String,
    val colorHex: String,
    val iconKey: String? = null,
    /** Transactions filed under it, so the picker can rank it alongside the built-in categories. */
    val useCount: Int = 0,
)

/**
 * The key merchant rules match a payment on: its name as captured, normalised. Null for a payment
 * that names no counterparty, such as an ATM withdrawal.
 */
val TransactionEntity.merchantKey: String? get() = Categorizer.merchantKey(merchantRaw)

/** One merchant the user has paid, for the Merchants list. */
data class MerchantSummary(
    val key: String,
    /** What its most recent payment is shown as. */
    val name: String,
    /** The category its most recent payment is filed under, for the row's icon. */
    val category: Category,
    val paymentCount: Int,
    val spentMinor: Long,
    val lastPaidAt: Long,
)

/**
 * Every merchant paid in [transactions], most-paid first. Income is left out: this lists who the
 * money went to, and a refund from a merchant is not a payment to it.
 */
fun merchantSummaries(transactions: List<TransactionEntity>): List<MerchantSummary> =
    transactions
        .filter { it.direction == Direction.DEBIT }
        .groupBy { it.merchantKey }
        .mapNotNull { (key, payments) ->
            key ?: return@mapNotNull null
            val latest = payments.maxBy { it.occurredAt }
            MerchantSummary(
                key = key,
                name = latest.merchant,
                category = latest.category,
                paymentCount = payments.size,
                spentMinor = payments.sumOf { it.amountMinor },
                lastPaidAt = latest.occurredAt,
            )
        }
        .sortedWith(compareByDescending<MerchantSummary> { it.paymentCount }.thenByDescending { it.lastPaidAt })

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val categorizer: Categorizer,
    private val contactNameCacheDao: ContactNameCacheDao? = null,
    private val contactResolver: ContactResolver? = null,
    private val budgetNotifier: BudgetNotifier? = null,
    private val transactionNotifier: TransactionNotifier? = null,
) {

    fun observeAll(): Flow<List<TransactionEntity>> = transactionDao.observeAll()

    fun observeSince(from: Long): Flow<List<TransactionEntity>> = transactionDao.observeSince(from)

    fun observeBetween(from: Long, to: Long): Flow<List<TransactionEntity>> =
        transactionDao.observeBetween(from, to)

    fun observeCount(): Flow<Int> = transactionDao.observeCount()

    suspend fun allTransactions(): List<TransactionEntity> = transactionDao.allOnce()

    /** Every tag in use across all history, most-used first, for tag suggestions. */
    fun observeTagUsage(): Flow<List<TagUsage>> = transactionDao.observeAll().map { tagUsageOf(it) }

    fun observeMerchants(): Flow<List<MerchantSummary>> = transactionDao.observeAll().map(::merchantSummaries)

    /** Tags ordered by how often they're used, so recent/common ones surface first as suggestions. */
    fun observeTagSuggestions(): Flow<List<String>> = observeTagUsage().map { usages -> usages.map { it.tag } }

    /** Custom categories the user has created before, most recently used first, for reuse in the picker. */
    fun observeCustomCategorySuggestions(): Flow<List<CustomCategoryOption>> = transactionDao.observeAll().map { transactions ->
        val seen = LinkedHashMap<String, CustomCategoryOption>()
        val counts = HashMap<String, Int>()
        transactions.sortedByDescending { it.occurredAt }.forEach { tx ->
            val name = tx.customCategoryName?.trim()
            val colorHex = tx.customCategoryColor
            if (!name.isNullOrEmpty() && colorHex != null) {
                val key = name.lowercase()
                counts[key] = (counts[key] ?: 0) + 1
                // The most recent transaction decides its look, as it always has.
                if (!seen.containsKey(key)) seen[key] = CustomCategoryOption(name, colorHex, tx.customCategoryIcon)
            }
        }
        seen.map { (key, option) -> option.copy(useCount = counts[key] ?: 0) }
    }

    /** How many transactions currently wear a custom category, so the user can be told before it goes. */
    suspend fun countWithCustomCategory(name: String): Int =
        transactionDao.countWithCustomCategory(name)

    /**
     * Removes a custom category, re-filing its transactions under [fallback].
     *
     * The category disappears from the picker as a consequence rather than by being deleted: the
     * list is derived from what transactions carry, so once nothing carries the name it is gone.
     * Nothing is deleted here except the label — every transaction, its amount, date and merchant,
     * survives with a category it can be found under.
     */
    suspend fun deleteCustomCategory(name: String, fallback: Category = Category.OTHER) {
        transactionDao.clearCustomCategory(name, fallback)
    }

    /** Renames and restyles a custom category on every transaction that carries it. */
    suspend fun updateCustomCategory(name: String, newName: String, colorHex: String, iconKey: String) {
        transactionDao.updateCustomCategory(name, newName.trim(), colorHex, iconKey)
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
        if (customCategoryName == null) categorizer.learn(merchant, direction, category)
        categorizer.learnTags(merchant, direction, tags)
        return transactionDao.insert(entity).also { id ->
            budgetNotifier?.onTransactionRecorded(entity.copy(id = id))
        }
    }

    /**
     * Stores a captured payment. Returns the new row id, or null when it was a duplicate: one
     * payment routinely reaches us twice, as an app notification and again as a bank alert.
     */
    suspend fun ingest(payment: ParsedPayment, captureSource: CaptureSource): Long? {
        val dedupeKey = dedupeKey(payment)
        if (transactionDao.findByDedupeKey(dedupeKey) != null) {
            Log.d(TAG, "Skipping duplicate capture: same dedupe key already stored")
            return null
        }

        // A shared reference id is the strongest duplicate signal, so it wins regardless of timing.
        payment.referenceId?.takeIf { it.isNotBlank() }?.let { reference ->
            transactionDao.findByReference(reference)?.let { existing ->
                Log.d(TAG, "Skipping duplicate of ${existing.id}: same reference $reference")
                return null
            }
        }

        // Same amount and direction close together, by when it happened or by when it reached us,
        // is a second sighting unless the two name different accounts, banks or references.
        val window = TimeUnit.MINUTES.toMillis(DEDUPE_WINDOW_MINUTES)
        val captureWindow = TimeUnit.MINUTES.toMillis(CAPTURE_DEDUPE_WINDOW_MINUTES)
        val now = System.currentTimeMillis()
        val nearby = transactionDao.findSimilar(
            amountMinor = payment.amountMinor,
            direction = payment.direction.name,
            from = payment.occurredAt - window,
            to = payment.occurredAt + window,
        ) + transactionDao.findRecentlyCaptured(
            amountMinor = payment.amountMinor,
            direction = payment.direction.name,
            from = now - captureWindow,
            to = now + captureWindow,
        )
        nearby.firstOrNull { !CaptureDedupe.isDistinct(it, payment) }?.let { existing ->
            Log.d(TAG, "Skipping near-duplicate of transaction ${existing.id}")
            val enriched = CaptureDedupe.withDetailsFrom(existing, payment)
            if (enriched != existing) transactionDao.update(enriched)
            return null
        }

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
        // ATM withdrawals, interest and charges name no counterparty; their type is the clearest title.
        val merchantName = guess.merchantDisplayName ?: contactName ?: originalMerchant
            ?: payment.transactionType?.displayName ?: payment.sourceApp
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
            tags = guess.tags,
            paymentMode = PaymentModeDetector.detect(payment),
            dedupeKey = dedupeKey,
            bankName = payment.bankName,
            accountType = payment.accountType,
            accountLast4 = payment.accountLast4,
            transactionType = payment.transactionType,
            availableBalanceMinor = payment.availableBalanceMinor,
        )
        // The unique index is the last line of defence: two capture paths can race here.
        // Only captured payments alert: a manual entry is added while the user is already in the app.
        return transactionDao.insert(entity).takeIf { it > 0 }
            ?.also { id ->
                val recorded = entity.copy(id = id)
                transactionNotifier?.onTransactionRecorded(recorded)
                budgetNotifier?.onTransactionRecorded(recorded)
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
            categorizer.learn(existing.merchantRaw ?: existing.merchant, existing.direction, category)
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
        categorizer.learnTags(existing.merchantRaw ?: existing.merchant, existing.direction, cleaned)
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
                merchantRaw = rawAfterRename(existing, merchant),
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
                merchantRaw = rawAfterRename(existing, trimmedMerchant),
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
        // Learn against the saved direction, so a transaction edited into income teaches nothing.
        val learnFrom = existing.merchantRaw ?: existing.merchant
        if (categoryChanged && customCategoryName == null) {
            categorizer.learn(learnFrom, direction, category)
        }
        if (merchantRenamed) {
            categorizer.learnDisplayName(learnFrom, direction, trimmedMerchant)
        }
        categorizer.learnTags(learnFrom, direction, cleanedTags)
    }

    suspend fun delete(id: Long) = transactionDao.delete(id)

    /**
     * A captured payment keeps the name it arrived with when renamed: that is what its merchant
     * rules match on, so later edits to it keep teaching the same merchant. A manual entry's name
     * was typed in the first place, so renaming one simply replaces it.
     */
    private fun rawAfterRename(existing: TransactionEntity, merchant: String): String? =
        if (existing.captureSource == CaptureSource.MANUAL || existing.merchantRaw.isNullOrBlank()) merchant
        else existing.merchantRaw

    /**
     * A stable fingerprint for one payment, so the same payment seen twice produces the same key
     * and the unique index rejects the second copy. Falls back to amount + merchant + a coarse
     * time bucket when the alert carries no reference id.
     */
    private fun dedupeKey(payment: ParsedPayment): String {
        val reference = payment.referenceId?.lowercase()
        val raw = if (reference != null) {
            "ref:$reference"
        } else {
            val bucket = payment.occurredAt / TimeUnit.MINUTES.toMillis(DEDUPE_WINDOW_MINUTES)
            // The account keeps two same-amount payments to one merchant from two accounts apart.
            "amt:${payment.amountMinor}|dir:${payment.direction}|m:${Categorizer.merchantKey(payment.merchantRaw)}|t:$bucket" +
                "|a:${payment.bankName.orEmpty()}:${payment.accountLast4.orEmpty()}"
        }
        return MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val TAG = "TransactionRepository"
        const val DEDUPE_WINDOW_MINUTES = 3L

        /** Duplicate alerts for one payment land within seconds; a few minutes is a safe net. */
        const val CAPTURE_DEDUPE_WINDOW_MINUTES = 5L
    }
}
