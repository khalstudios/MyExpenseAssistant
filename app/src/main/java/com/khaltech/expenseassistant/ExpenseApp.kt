package com.khaltech.expenseassistant

import android.app.Application
import com.khaltech.expenseassistant.di.ServiceLocator
import com.khaltech.expenseassistant.notify.BudgetNotifier

class ExpenseApp : Application() {
    override fun onCreate() {
        super.onCreate()
        BudgetNotifier.createChannel(this)
        ServiceLocator.repository(this)
    }
}
