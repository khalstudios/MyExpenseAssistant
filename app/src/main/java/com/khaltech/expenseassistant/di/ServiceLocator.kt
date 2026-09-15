package com.khaltech.expenseassistant.di

import android.content.Context
import com.khaltech.expenseassistant.categorize.Categorizer
import com.khaltech.expenseassistant.data.backup.BackupArchive
import com.khaltech.expenseassistant.data.local.AppDatabase
import com.khaltech.expenseassistant.data.prefs.CategoryIconStore
import com.khaltech.expenseassistant.data.prefs.UserPreferences
import com.khaltech.expenseassistant.data.repo.BudgetRepository
import com.khaltech.expenseassistant.data.repo.ContactResolver
import com.khaltech.expenseassistant.data.repo.TransactionRepository
import com.khaltech.expenseassistant.notify.BudgetNotifier
import com.khaltech.expenseassistant.notify.TransactionNotifier

object ServiceLocator {

    @Volatile private var repository: TransactionRepository? = null
    @Volatile private var budgets: BudgetRepository? = null
    @Volatile private var preferences: UserPreferences? = null
    @Volatile private var categoryIcons: CategoryIconStore? = null
    @Volatile private var backupArchive: BackupArchive? = null

    fun repository(context: Context): TransactionRepository = repository ?: synchronized(this) {
        repository ?: run {
            val db = AppDatabase.get(context)
            TransactionRepository(
                transactionDao = db.transactionDao(),
                categorizer = Categorizer(db.merchantRuleDao()),
                contactNameCacheDao = db.contactNameCacheDao(),
                contactResolver = ContactResolver(context.applicationContext),
                budgetNotifier = BudgetNotifier(
                    context.applicationContext,
                    db.transactionDao(),
                    db.budgetDao(),
                ),
                transactionNotifier = TransactionNotifier(context.applicationContext),
            ).also { repository = it }
        }
    }

    fun budgetRepository(context: Context): BudgetRepository = budgets ?: synchronized(this) {
        budgets ?: BudgetRepository(AppDatabase.get(context).budgetDao()).also { budgets = it }
    }

    fun userPreferences(context: Context): UserPreferences = preferences ?: synchronized(this) {
        preferences ?: UserPreferences(context).also { preferences = it }
    }

    fun categoryIconStore(context: Context): CategoryIconStore = categoryIcons ?: synchronized(this) {
        categoryIcons ?: CategoryIconStore(context).also { categoryIcons = it }
    }

    fun backupArchive(context: Context): BackupArchive = backupArchive ?: synchronized(this) {
        backupArchive ?: BackupArchive(
            database = AppDatabase.get(context),
            preferences = userPreferences(context),
            categoryIcons = categoryIconStore(context),
        ).also { backupArchive = it }
    }
}
