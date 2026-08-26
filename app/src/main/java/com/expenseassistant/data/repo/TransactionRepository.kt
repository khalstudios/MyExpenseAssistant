package com.expenseassistant.data.repo

import android.util.Log
import com.expenseassistant.categorize.Categorizer
import com.expenseassistant.data.local.TransactionDao
import com.expenseassistant.data.model.CaptureSource
import com.expenseassistant.data.model.Category
import com.expenseassistant.data.model.Direction
import com.expenseassistant.data.model.PaymentMode
import com.expenseassistant.data.model.TransactionEntity
import com.expenseassistant.parser.ParsedPayment
import com.expenseassistant.parser.PaymentModeDetector
import kotlinx.coroutines.flow.Flow
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val categorizer: Categorizer,
) {

    fun observeAll(): Flow<List<TransactionEntity>> = transactionDao.observeAll()

    fun observeSince(from: Long): Flow<List<TransactionEntity>> = transactionDao.observeSince(from)

    fun observeBetween(from: Long, to: Long): Flow<List<TransactionEntity>> =
        transactionDao.observeBetween(from, to)

    fun observeCount(): Flow<Int> = transactionDao.observeCount()

    suspend fun earliestTimestamp(): Long? = transactionDao.earliestTimestamp()

    suspend fun deleteAll() = transactionDao.deleteAll()

    suspend fun clearLearnedRules() = categorizer.forgetAll()

    /** Adds a transaction the capture services could not see, such as cash or a card swipe. */
    suspend fun addManual(
        amountMinor: Long,
        direction: Direction,
        merchant: String,
        category: Category,
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
        )
        categorizer.learn(merchant, category)
        return transactionDao.insert(entity)
    }

    /**
     * Stores a captured payment. Returns the new row id, or null when it was a duplicate
     * (the same payment often arrives as both a notification and a success screen).
     */
    suspend fun ingest(payment: ParsedPayment, captureSource: CaptureSource): Long? {
        val dedupeKey = dedupeKey(payment)
        if (transactionDao.findByDedupeKey(dedupeKey) != null) return null

        val window = TimeUnit.MINUTES.toMillis(DEDUPE_WINDOW_MINUTES)
        val similar = transactionDao.findSimilar(
            amountMinor = payment.amountMinor,
            direction = payment.direction.name,
            from = payment.occurredAt - window,
            to = payment.occurredAt + window,
        )
        if (similar != null) {
            Log.d(TAG, "Skipping near-duplicate of transaction ${similar.id}")
            return null
        }

        val guess = categorizer.categorize(payment)
        val entity = TransactionEntity(
            amountMinor = payment.amountMinor,
            currency = payment.currency,
            direction = payment.direction,
            merchantRaw = payment.merchantRaw,
            merchant = payment.merchantRaw?.trim()?.takeIf { it.isNotEmpty() } ?: payment.sourceApp,
            category = guess.category,
            categoryConfidence = guess.confidence,
            sourcePackage = payment.sourcePackage,
            sourceApp = payment.sourceApp,
            captureSource = captureSource,
            rawText = payment.rawText,
            referenceId = payment.referenceId,
            occurredAt = payment.occurredAt,
            paymentMode = PaymentModeDetector.detect(payment.rawText, payment.sourcePackage),
            dedupeKey = dedupeKey,
        )
        return transactionDao.insert(entity).takeIf { it > 0 }
    }

    fun observeById(id: Long): Flow<TransactionEntity?> = transactionDao.observeById(id)

    suspend fun recategorize(id: Long, category: Category) {
        val existing = transactionDao.findById(id) ?: return
        transactionDao.update(
            existing.copy(category = category, categoryConfidence = 1f, userCorrected = true)
        )
        categorizer.learn(existing.merchantRaw ?: existing.merchant, category)
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

    suspend fun delete(id: Long) = transactionDao.delete(id)

    private fun dedupeKey(payment: ParsedPayment): String {
        val reference = payment.referenceId?.lowercase()
        val raw = if (reference != null) {
            "ref:$reference"
        } else {
            val bucket = payment.occurredAt / TimeUnit.MINUTES.toMillis(DEDUPE_WINDOW_MINUTES)
            "amt:${payment.amountMinor}|dir:${payment.direction}|m:${Categorizer.merchantKey(payment.merchantRaw)}|t:$bucket"
        }
        return MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val TAG = "TransactionRepository"
        const val DEDUPE_WINDOW_MINUTES = 3L
    }
}
