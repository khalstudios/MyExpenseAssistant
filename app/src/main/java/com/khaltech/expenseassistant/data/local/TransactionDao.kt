package com.khaltech.expenseassistant.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.khaltech.expenseassistant.data.model.Category
import com.khaltech.expenseassistant.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM transactions ORDER BY occurredAt DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY occurredAt DESC")
    suspend fun allOnce(): List<TransactionEntity>

    @Query(
        """
        SELECT COALESCE(SUM(amountMinor), 0) FROM transactions
        WHERE direction = 'DEBIT' AND occurredAt >= :from AND occurredAt < :to
        """
    )
    suspend fun spendBetween(from: Long, to: Long): Long

    @Query(
        """
        SELECT COALESCE(SUM(amountMinor), 0) FROM transactions
        WHERE direction = 'DEBIT' AND category = :category AND occurredAt >= :from AND occurredAt < :to
        """
    )
    suspend fun categorySpendBetween(category: Category, from: Long, to: Long): Long

    @Query("SELECT * FROM transactions WHERE occurredAt >= :from ORDER BY occurredAt DESC")
    fun observeSince(from: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE occurredAt >= :from AND occurredAt < :to ORDER BY occurredAt DESC")
    fun observeBetween(from: Long, to: Long): Flow<List<TransactionEntity>>

    @Query("SELECT COUNT(*) FROM transactions")
    fun observeCount(): Flow<Int>

    @Query("SELECT MIN(occurredAt) FROM transactions")
    suspend fun earliestTimestamp(): Long?

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun findById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun observeById(id: Long): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions WHERE dedupeKey = :key LIMIT 1")
    suspend fun findByDedupeKey(key: String): TransactionEntity?

    /** Near-duplicate guard: same amount and direction captured within a short window. */
    @Query(
        """
        SELECT * FROM transactions
        WHERE amountMinor = :amountMinor
          AND direction = :direction
          AND occurredAt BETWEEN :from AND :to
        LIMIT 1
        """
    )
    suspend fun findSimilar(amountMinor: Long, direction: String, from: Long, to: Long): TransactionEntity?

    /**
     * Second dedupe pass keyed on when we *captured* the row rather than when the payment happened.
     * The bank SMS and the UPI app notification for one payment arrive seconds apart, but their
     * parsed occurredAt can differ by hours when one of them spells out a date.
     */
    @Query(
        """
        SELECT * FROM transactions
        WHERE amountMinor = :amountMinor
          AND direction = :direction
          AND createdAt BETWEEN :from AND :to
        LIMIT 1
        """
    )
    suspend fun findRecentlyCaptured(amountMinor: Long, direction: String, from: Long, to: Long): TransactionEntity?

    /** Any earlier row carrying the same bank/UPI reference, whatever the amount or timing. */
    @Query("SELECT * FROM transactions WHERE referenceId = :referenceId LIMIT 1")
    suspend fun findByReference(referenceId: String): TransactionEntity?

    /**
     * Drops a category the user made, re-filing everything under it as [fallback].
     *
     * A custom category is not a row anywhere: it exists because transactions carry its name and
     * look. Clearing those three columns is what deleting it means, and every transaction wearing
     * it has to be given a real category in the same statement or it would be left naming a
     * category that no longer exists.
     *
     * TRIM and NOCASE because the picker trims and lowercases names when it lists them, so "Gym",
     * "gym" and "Gym " are one category there and must be one here too.
     */
    @Query(
        """
        UPDATE transactions
        SET customCategoryName = NULL, customCategoryColor = NULL, customCategoryIcon = NULL,
            category = :fallback
        WHERE TRIM(customCategoryName) = TRIM(:name) COLLATE NOCASE
        """
    )
    suspend fun clearCustomCategory(name: String, fallback: Category)

    /**
     * Renames and restyles a category the user made. Like deleting one, this is an edit to every
     * transaction carrying it, since that is the only place a custom category lives.
     */
    @Query(
        """
        UPDATE transactions
        SET customCategoryName = :newName, customCategoryColor = :colorHex, customCategoryIcon = :iconKey
        WHERE TRIM(customCategoryName) = TRIM(:name) COLLATE NOCASE
        """
    )
    suspend fun updateCustomCategory(name: String, newName: String, colorHex: String, iconKey: String)

    @Query("SELECT COUNT(*) FROM transactions WHERE TRIM(customCategoryName) = TRIM(:name) COLLATE NOCASE")
    suspend fun countWithCustomCategory(name: String): Int

    @Query("UPDATE transactions SET category = :category, userCorrected = 1 WHERE id = :id")
    suspend fun setCategory(id: Long, category: Category)
}
