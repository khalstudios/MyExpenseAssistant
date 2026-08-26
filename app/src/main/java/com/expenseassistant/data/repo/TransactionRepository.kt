package com.expenseassistant.data.repo

import android.util.Log
import com.expenseassistant.categorize.Categorizer
import com.expenseassistant.data.local.TransactionDao
import com.expenseassistant.data.model.CaptureSource
import com.expenseassistant.data.model.Category
import com.expenseassistant.data.model.TransactionEntity
import com.expenseassistant.parser.ParsedPayment
import kotlinx.coroutines.flow.Flow
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val categorizer: Categorizer,
) {

    fun observeAll(): Flow<List<TransactionEntity>> = transactionDao.observeAll()

    fun observeSince(from: Long): Flow<List<TransactionEntity>> = transactionDao.observeSince(from)

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
            dedupeKey = dedupeKey,
        )
        return transactionDao.insert(entity).takeIf { it > 0 }
    }

    suspend fun recategorize(id: Long, category: Category) {
        val existing = transactionDao.findById(id) ?: return
        transactionDao.update(
            existing.copy(category = category, categoryConfidence = 1f, userCorrected = true)
        )
        categorizer.learn(existing.merchantRaw ?: existing.merchant, category)
    }

    suspend fun updateNote(id: Long, note: String?) {
        val existing = transactionDao.findById(id) ?: return
        transactionDao.update(existing.copy(note = note?.takeIf { it.isNotBlank() }))
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
