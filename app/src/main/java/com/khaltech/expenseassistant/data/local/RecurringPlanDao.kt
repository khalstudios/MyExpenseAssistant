package com.khaltech.expenseassistant.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.khaltech.expenseassistant.data.model.RecurringPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringPlanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(plan: RecurringPlanEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(plans: List<RecurringPlanEntity>)

    @Query("SELECT * FROM recurring_plans ORDER BY amountMinor DESC")
    fun observeAll(): Flow<List<RecurringPlanEntity>>

    @Query("SELECT * FROM recurring_plans ORDER BY amountMinor DESC")
    suspend fun allOnce(): List<RecurringPlanEntity>

    @Query("DELETE FROM recurring_plans WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM recurring_plans")
    suspend fun deleteAll()
}
