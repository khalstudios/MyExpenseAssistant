package com.expenseassistant.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.expenseassistant.data.model.Category
import com.expenseassistant.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM transactions ORDER BY occurredAt DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE occurredAt >= :from ORDER BY occurredAt DESC")
    fun observeSince(from: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun findById(id: Long): TransactionEntity?

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

    @Query("UPDATE transactions SET category = :category, userCorrected = 1 WHERE id = :id")
    suspend fun setCategory(id: Long, category: Category)
}
