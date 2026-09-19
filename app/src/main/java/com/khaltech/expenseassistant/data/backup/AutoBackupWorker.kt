package com.khaltech.expenseassistant.data.backup

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.khaltech.expenseassistant.data.prefs.UserPreferences
import com.khaltech.expenseassistant.di.ServiceLocator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class AutoBackupWorker(appContext: Context, parameters: WorkerParameters) : CoroutineWorker(appContext, parameters) {
    override suspend fun doWork(): Result {
        // Checked on every run, not only when backups are switched on: the entitlement can lapse
        // long after the schedule was enqueued, and this is the last point before data is written.
        if (!AutoBackupScheduler.enforceEntitlement(applicationContext)) return Result.success()

        val settings = UserPreferences(applicationContext).autoBackupSettings() ?: return Result.success()
        return try {
            withContext(Dispatchers.IO) {
                BackupFolder.write(
                    applicationContext,
                    Uri.parse(settings.folderUri),
                    BackupArchive.fileName("kahan-gaya-paisa-auto"),
                    ServiceLocator.backupArchive(applicationContext).export(),
                )
            }
            Result.success()
        } catch (_: SecurityException) {
            Result.failure()
        } catch (_: IOException) {
            Result.retry()
        }
    }
}