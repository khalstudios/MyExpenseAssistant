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

    @Query("UPDATE transactions SET category = :category, userCorrected = 1 WHERE id = :id")
    suspend fun setCategory(id: Long, category: Category)
}
