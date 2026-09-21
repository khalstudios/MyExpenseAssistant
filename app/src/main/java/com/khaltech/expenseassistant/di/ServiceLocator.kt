package com.khaltech.expenseassistant.di

import android.content.Context
import com.khaltech.expenseassistant.billing.BillingManager
import com.khaltech.expenseassistant.billing.EntitlementStore
import com.khaltech.expenseassistant.categorize.Categorizer
import com.khaltech.expenseassistant.data.backup.BackupArchive
import com.khaltech.expenseassistant.data.local.AppDatabase
import com.khaltech.expenseassistant.data.prefs.CategoryColorStore
import com.khaltech.expenseassistant.data.prefs.CategoryIconStore
import com.khaltech.expenseassistant.data.prefs.UserPreferences
import com.khaltech.expenseassistant.data.repo.BudgetRepository
import com.khaltech.expenseassistant.data.repo.ContactResolver
import com.khaltech.expenseassistant.data.repo.RecurringPlanRepository
import com.khaltech.expenseassistant.data.repo.TransactionRepository
import com.khaltech.expenseassistant.notify.BudgetNotifier
import com.khaltech.expenseassistant.notify.TransactionNotifier

object ServiceLocator {

    @Volatile private var repository: TransactionRepository? = null
    @Volatile private var budgets: BudgetRepository? = null
    @Volatile private var recurringPlans: RecurringPlanRepository? = null
    @Volatile private var preferences: UserPreferences? = null
    @Volatile private var categoryIcons: CategoryIconStore? = null
    @Volatile private var categoryColors: CategoryColorStore? = null
    @Volatile private var backupArchive: BackupArchive? = null
    @Volatile private var entitlements: EntitlementStore? = null
    @Volatile private var billing: BillingManager? = null

    fun repository(context: Context): TransactionRepository = repository ?: synchronized(this) {
        repository ?: run {
            val db = AppDatabase.get(context)
            TransactionRepository(
                transactionDao = db.transactionDao(),
                categorizer = Categorizer(
                    db.merchantRuleDao(),
                    // Local and cheap, so capture never waits on Play to decide a category.
                    isPro = { entitlementStore(context).isPro() },
                ),
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

    fun recurringPlanRepository(context: Context): RecurringPlanRepository = recurringPlans ?: synchronized(this) {
        recurringPlans ?: AppDatabase.get(context).let { db ->
            RecurringPlanRepository(db.recurringPlanDao(), db.recurringDismissalDao())
        }.also { recurringPlans = it }
    }

    fun userPreferences(context: Context): UserPreferences = preferences ?: synchronized(this) {
        preferences ?: UserPreferences(context).also { preferences = it }
    }

    fun categoryIconStore(context: Context): CategoryIconStore = categoryIcons ?: synchronized(this) {
        categoryIcons ?: CategoryIconStore(context).also { categoryIcons = it }
    }

    fun categoryColorStore(context: Context): CategoryColorStore = categoryColors ?: synchronized(this) {
        categoryColors ?: CategoryColorStore(context).also { categoryColors = it }
    }

    fun backupArchive(context: Context): BackupArchive = backupArchive ?: synchronized(this) {
        backupArchive ?: BackupArchive(
            database = AppDatabase.get(context),
            preferences = userPreferences(context),
            categoryIcons = categoryIconStore(context),
            categoryColors = categoryColorStore(context),
        ).also { backupArchive = it }
    }

    fun entitlementStore(context: Context): EntitlementStore = entitlements ?: synchronized(this) {
        entitlements ?: EntitlementStore(context).also { entitlements = it }
    }

    fun billing(context: Context): BillingManager = billing ?: synchronized(this) {
        billing ?: BillingManager(context, entitlementStore(context)).also { billing = it }
    }
}
