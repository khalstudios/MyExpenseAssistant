package com.expenseassistant.di

import android.content.Context
import com.expenseassistant.categorize.Categorizer
import com.expenseassistant.data.local.AppDatabase
import com.expenseassistant.data.prefs.CategoryIconStore
import com.expenseassistant.data.prefs.UserPreferences
import com.expenseassistant.data.repo.BudgetRepository
import com.expenseassistant.data.repo.ContactResolver
import com.expenseassistant.data.repo.TransactionRepository
import com.expenseassistant.notify.BudgetNotifier

object ServiceLocator {

    @Volatile private var repository: TransactionRepository? = null
    @Volatile private var budgets: BudgetRepository? = null
    @Volatile private var preferences: UserPreferences? = null
    @Volatile private var categoryIcons: CategoryIconStore? = null

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
}
