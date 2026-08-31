package com.expenseassistant.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.expenseassistant.data.model.BudgetEntity
import com.expenseassistant.data.model.MerchantRule
import com.expenseassistant.data.model.TransactionEntity

@Database(
    entities = [TransactionEntity::class, MerchantRule::class, BudgetEntity::class],
    version = 6,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun merchantRuleDao(): MerchantRuleDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE transactions ADD COLUMN paymentMode TEXT NOT NULL DEFAULT 'UNKNOWN'")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS budgets (
                        categoryKey TEXT NOT NULL PRIMARY KEY,
                        limitMinor INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN customCategoryName TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN customCategoryColor TEXT")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN customCategoryIcon TEXT")
            }
        }

        // House Expense and Vehicle Expense merged into one Maintenance category.
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "UPDATE transactions SET category = 'MAINTENANCE' " +
                        "WHERE category IN ('HOUSE_MAINTENANCE', 'VEHICLE_MAINTENANCE')"
                )
                db.execSQL(
                    "UPDATE merchant_rules SET category = 'MAINTENANCE' " +
                        "WHERE category IN ('HOUSE_MAINTENANCE', 'VEHICLE_MAINTENANCE')"
                )
                // Keep whichever budget existed; a duplicate key would violate the primary key.
                db.execSQL("DELETE FROM budgets WHERE categoryKey = 'VEHICLE_MAINTENANCE' AND EXISTS (SELECT 1 FROM budgets WHERE categoryKey = 'HOUSE_MAINTENANCE')")
                db.execSQL("UPDATE budgets SET categoryKey = 'MAINTENANCE' WHERE categoryKey IN ('HOUSE_MAINTENANCE', 'VEHICLE_MAINTENANCE')")
            }
        }

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "expense-assistant.db",
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .build().also { instance = it }
        }
    }
}
