package com.expenseassistant.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.expenseassistant.data.model.MerchantRule

@Dao
interface MerchantRuleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: MerchantRule)

    @Query("SELECT * FROM merchant_rules WHERE merchantKey = :key LIMIT 1")
    suspend fun find(key: String): MerchantRule?

    @Query("SELECT * FROM merchant_rules")
    suspend fun all(): List<MerchantRule>
}
