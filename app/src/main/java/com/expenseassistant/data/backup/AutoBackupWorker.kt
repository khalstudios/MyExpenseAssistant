package com.expenseassistant.data.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.expenseassistant.data.prefs.UserPreferences
import com.expenseassistant.di.ServiceLocator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class AutoBackupWorker(appContext: Context, parameters: WorkerParameters) : CoroutineWorker(appContext, parameters) {
    override suspend fun doWork(): Result {
        val settings = UserPreferences(applicationContext).autoBackupSettings() ?: return Result.success()
        return try {
            withContext(Dispatchers.IO) {
                val directory = DocumentFile.fromTreeUri(applicationContext, Uri.parse(settings.folderUri))
                    ?: throw IOException("Backup folder is unavailable")
                val fileName = BackupArchive.fileName("expense-assistant-auto")
                val destination = directory.createFile("application/json", fileName)
                    ?: throw IOException("Could not create backup file")
                applicationContext.contentResolver.openOutputStream(destination.uri)?.bufferedWriter()?.use { writer ->
                    writer.write(ServiceLocator.backupArchive(applicationContext).export())
                } ?: throw IOException("Could not write backup file")
            }
            Result.success()
        } catch (_: SecurityException) {
            Result.failure()
        } catch (_: IOException) {
            Result.retry()
        }
    }
}