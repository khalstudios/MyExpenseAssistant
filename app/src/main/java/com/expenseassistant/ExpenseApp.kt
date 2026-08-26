package com.expenseassistant

import android.app.Application
import com.expenseassistant.di.ServiceLocator

class ExpenseApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.repository(this)
    }
}
