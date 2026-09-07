package com.expenseassistant.data.backup

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.expenseassistant.data.prefs.AutoBackupSettings
import java.util.concurrent.TimeUnit

object AutoBackupScheduler {
    private const val WORK_NAME = "expense-assistant-auto-backup"

    fun schedule(context: Context, settings: AutoBackupSettings) {
        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(settings.interval.days, TimeUnit.DAYS).build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(WORK_NAME)
    }
}