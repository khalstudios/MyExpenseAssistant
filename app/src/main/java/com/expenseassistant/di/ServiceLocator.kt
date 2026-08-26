package com.expenseassistant.di

import android.content.Context
import com.expenseassistant.categorize.Categorizer
import com.expenseassistant.data.local.AppDatabase
import com.expenseassistant.data.repo.TransactionRepository

object ServiceLocator {

    @Volatile private var repository: TransactionRepository? = null

    fun repository(context: Context): TransactionRepository = repository ?: synchronized(this) {
        repository ?: run {
            val db = AppDatabase.get(context)
            TransactionRepository(db.transactionDao(), Categorizer(db.merchantRuleDao()))
                .also { repository = it }
        }
    }
}
