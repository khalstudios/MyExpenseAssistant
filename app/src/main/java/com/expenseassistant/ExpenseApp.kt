package com.expenseassistant

import android.app.Application
import com.expenseassistant.di.ServiceLocator
import com.expenseassistant.notify.BudgetNotifier

class ExpenseApp : Application() {
    override fun onCreate() {
        super.onCreate()
        BudgetNotifier.createChannel(this)
        ServiceLocator.repository(this)
    }
}
