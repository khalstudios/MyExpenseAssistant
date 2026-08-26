package com.expenseassistant.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.expenseassistant.data.model.MerchantRule
import com.expenseassistant.data.model.TransactionEntity

@Database(
    entities = [TransactionEntity::class, MerchantRule::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun merchantRuleDao(): MerchantRuleDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "expense-assistant.db",
            ).build().also { instance = it }
        }
    }
}
