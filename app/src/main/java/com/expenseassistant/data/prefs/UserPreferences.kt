package com.expenseassistant.data.prefs

import android.content.Context

data class UserProfile(
    val name: String = "",
    val email: String = "",
    val monthlyIncomeMinor: Long = 0,
)

enum class BackupInterval(val days: Long, val label: String) {
    FIFTEEN_DAYS(15, "Every 15 days"),
    MONTHLY(30, "Monthly"),
}

data class AutoBackupSettings(
    val interval: BackupInterval,
    val folderUri: String,
)

class UserPreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("user-profile", Context.MODE_PRIVATE)

    fun load(): UserProfile = UserProfile(
        name = prefs.getString(KEY_NAME, "").orEmpty(),
        email = prefs.getString(KEY_EMAIL, "").orEmpty(),
        monthlyIncomeMinor = prefs.getLong(KEY_INCOME, 0),
    )

    fun save(profile: UserProfile) {
        prefs.edit()
            .putString(KEY_NAME, profile.name)
            .putString(KEY_EMAIL, profile.email)
            .putLong(KEY_INCOME, profile.monthlyIncomeMinor)
            .apply()
    }

    fun autoBackupSettings(): AutoBackupSettings? {
        val interval = prefs.getString(KEY_BACKUP_INTERVAL, null)?.let { value ->
            BackupInterval.entries.firstOrNull { it.name == value }
        } ?: return null
        val folderUri = prefs.getString(KEY_BACKUP_FOLDER_URI, null) ?: return null
        return AutoBackupSettings(interval, folderUri)
    }

    fun saveAutoBackup(settings: AutoBackupSettings) {
        prefs.edit()
            .putString(KEY_BACKUP_INTERVAL, settings.interval.name)
            .putString(KEY_BACKUP_FOLDER_URI, settings.folderUri)
            .apply()
    }

    fun clearAutoBackup() {
        prefs.edit().remove(KEY_BACKUP_INTERVAL).remove(KEY_BACKUP_FOLDER_URI).apply()
    }

    fun isBackupNoticeDismissed(): Boolean = prefs.getBoolean(KEY_BACKUP_NOTICE_DISMISSED, false)

    fun dismissBackupNotice() {
        prefs.edit().putBoolean(KEY_BACKUP_NOTICE_DISMISSED, true).apply()
    }

    private companion object {
        const val KEY_NAME = "name"
        const val KEY_EMAIL = "email"
        const val KEY_INCOME = "monthly_income_minor"
        const val KEY_BACKUP_INTERVAL = "backup_interval"
        const val KEY_BACKUP_FOLDER_URI = "backup_folder_uri"
        const val KEY_BACKUP_NOTICE_DISMISSED = "backup_notice_dismissed"
    }
}
