package com.expenseassistant.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.expenseassistant.data.model.ContactNameCache

@Dao
interface ContactNameCacheDao {

    @Query("SELECT * FROM contact_name_cache WHERE merchantKey = :merchantKey LIMIT 1")
    suspend fun find(merchantKey: String): ContactNameCache?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: ContactNameCache)

    @Query("DELETE FROM contact_name_cache")
    suspend fun deleteAll()
}