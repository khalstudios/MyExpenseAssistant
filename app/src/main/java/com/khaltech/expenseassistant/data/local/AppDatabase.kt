package com.khaltech.expenseassistant.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.khaltech.expenseassistant.data.model.BudgetEntity
import com.khaltech.expenseassistant.data.model.ContactNameCache
import com.khaltech.expenseassistant.data.model.MerchantRule
import com.khaltech.expenseassistant.data.model.TransactionEntity

@Database(
    entities = [TransactionEntity::class, MerchantRule::class, BudgetEntity::class, ContactNameCache::class],
    version = 13,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun merchantRuleDao(): MerchantRuleDao
    abstract fun budgetDao(): BudgetDao
    abstract fun contactNameCacheDao(): ContactNameCacheDao

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

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE merchant_rules ADD COLUMN displayName TEXT")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS contact_name_cache (merchantKey TEXT NOT NULL PRIMARY KEY, contactName TEXT, lookedUpAt INTEGER NOT NULL)"
                )
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP INDEX IF EXISTS index_transactions_dedupeKey")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_dedupeKey ON transactions (dedupeKey)")
            }
        }

        // Merchant rules now also remember the tags and note last saved for that merchant.
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE merchant_rules ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE merchant_rules ADD COLUMN note TEXT")
            }
        }

        /**
         * Restores the unique dedupe index that version 9 downgraded to a plain one, which let the
         * same payment be stored once per capture path. Existing keys are random UUIDs, so the
         * cleanup below is a no-op in practice and only guards the index creation.
         */
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "DELETE FROM transactions WHERE id NOT IN " +
                        "(SELECT MIN(id) FROM transactions GROUP BY dedupeKey)"
                )
                db.execSQL("DROP INDEX IF EXISTS index_transactions_dedupeKey")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_transactions_dedupeKey ON transactions (dedupeKey)")
            }
        }

        /**
         * Maintenance is split into House Expense and Vehicle Expense. Nothing records which one an
         * old entry was, so everything moves to House Expense for the user to re-file as needed.
         */
        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE transactions SET category = 'HOUSE_EXPENSE' WHERE category = 'MAINTENANCE'")
                db.execSQL("UPDATE merchant_rules SET category = 'HOUSE_EXPENSE' WHERE category = 'MAINTENANCE'")
                db.execSQL("UPDATE budgets SET categoryKey = 'HOUSE_EXPENSE' WHERE categoryKey = 'MAINTENANCE'")
            }
        }

        // Bank alerts now record the bank, account, rail and balance they state. Older rows stay null.
        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN bankName TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN accountType TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN accountLast4 TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN transactionType TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN availableBalanceMinor INTEGER")
            }
        }

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "expense-assistant.db",
            ).addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
                MIGRATION_7_8,
                MIGRATION_8_9,
                MIGRATION_9_10,
                MIGRATION_10_11,
                MIGRATION_11_12,
                MIGRATION_12_13,
            )
                .build().also { instance = it }
        }
    }
}
