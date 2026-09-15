package com.khaltech.expenseassistant

import android.app.Application
import com.khaltech.expenseassistant.di.ServiceLocator
import com.khaltech.expenseassistant.notify.BudgetNotifier
import com.khaltech.expenseassistant.notify.TransactionNotifier

class ExpenseApp : Application() {
    override fun onCreate() {
        super.onCreate()
        BudgetNotifier.createChannel(this)
        TransactionNotifier.createChannel(this)
        ServiceLocator.repository(this)
    }
}
