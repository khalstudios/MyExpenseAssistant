package com.khaltech.expenseassistant.data.backup

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.khaltech.expenseassistant.data.prefs.AutoBackupSettings
import com.khaltech.expenseassistant.data.prefs.UserPreferences
import com.khaltech.expenseassistant.di.ServiceLocator
import java.util.concurrent.TimeUnit

object AutoBackupScheduler {
    private const val WORK_NAME = "expense-assistant-auto-backup"

    fun schedule(context: Context, settings: AutoBackupSettings) {
        // A periodic request runs its first time straight away unless delayed, which would make a
        // backup the moment the schedule is set. The first one is due a full interval from now.
        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(settings.interval.days, TimeUnit.DAYS)
            .setInitialDelay(settings.interval.days, TimeUnit.DAYS)
            .build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(WORK_NAME)
    }

    /**
     * Stops automatic backups if this install is no longer entitled to them, and reports whether
     * they may run. Pro can lapse after it was set up - a refund, or a yearly plan not renewed -
     * and a schedule enqueued while it was valid would otherwise keep firing forever.
     *
     * The stored settings are cleared along with the schedule so the Profile screen stops
     * advertising a cadence that is no longer running. The backup *location* is remembered
     * separately, so the list of existing backups and manual backups both survive this.
     *
     * Safe to call from anywhere, and cheap: the entitlement is read from local storage, never
     * from Play, so this works with no network and cannot revoke anything on its own.
     */
    fun enforceEntitlement(context: Context): Boolean {
        if (ServiceLocator.entitlementStore(context).isPro()) return true
        cancel(context)
        UserPreferences(context).clearAutoBackup()
        return false
    }
}