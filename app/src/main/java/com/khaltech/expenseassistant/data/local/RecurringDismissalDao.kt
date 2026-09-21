package com.khaltech.expenseassistant.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.khaltech.expenseassistant.data.model.RecurringDismissal
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringDismissalDao {

    /** Dismissing the same merchant twice simply moves the clock to the later moment. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(dismissal: RecurringDismissal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(dismissals: List<RecurringDismissal>)

    @Query("SELECT * FROM recurring_dismissals")
    fun observeAll(): Flow<List<RecurringDismissal>>

    @Query("SELECT * FROM recurring_dismissals")
    suspend fun allOnce(): List<RecurringDismissal>

    @Query("DELETE FROM recurring_dismissals WHERE merchantKey = :merchantKey")
    suspend fun delete(merchantKey: String)

    @Query("DELETE FROM recurring_dismissals")
    suspend fun deleteAll()
}
