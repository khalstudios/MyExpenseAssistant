package com.khaltech.expenseassistant.data.prefs

import android.content.Context

data class UserProfile(
    val name: String = "",
    val email: String = "",
    val monthlyIncomeMinor: Long = 0,
)

enum class BackupInterval(val days: Long, val label: String) {
    DAILY(1, "Daily"),
    WEEKLY(7, "Weekly"),
    BIWEEKLY(14, "Every 2 weeks"),
    MONTHLY(30, "Monthly"),
    ;

    companion object {
        /**
         * Builds before the daily and weekly options stored FIFTEEN_DAYS. Without this the value no
         * longer parses and automatic backups would quietly switch themselves off on upgrade.
         */
        private val LEGACY = mapOf("FIFTEEN_DAYS" to BIWEEKLY)

        fun fromStored(value: String): BackupInterval? =
            entries.firstOrNull { it.name == value } ?: LEGACY[value]
    }
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
        val interval = prefs.getString(KEY_BACKUP_INTERVAL, null)
            ?.let(BackupInterval::fromStored)
            ?: return null
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

    /**
     * The location the user last backed up to, kept separately from the automatic-backup folder so
     * turning automatic backups off does not lose the list of existing backups.
     */
    fun backupLocationUri(): String? = prefs.getString(KEY_BACKUP_LOCATION_URI, null)

    fun saveBackupLocationUri(uri: String) {
        prefs.edit().putString(KEY_BACKUP_LOCATION_URI, uri).apply()
    }

    fun isBackupNoticeDismissed(): Boolean = prefs.getBoolean(KEY_BACKUP_NOTICE_DISMISSED, false)

    fun dismissBackupNotice() {
        prefs.edit().putBoolean(KEY_BACKUP_NOTICE_DISMISSED, true).apply()
    }

    fun isTutorialSeen(): Boolean = prefs.getBoolean(KEY_TUTORIAL_SEEN, false)

    fun markTutorialSeen() {
        prefs.edit().putBoolean(KEY_TUTORIAL_SEEN, true).apply()
    }

    /** True once the start-of-day budget prompt has been shown for [dayKey]. */
    fun isDailyPromptSeen(dayKey: String): Boolean =
        prefs.getString(KEY_DAILY_PROMPT_DAY, null) == dayKey

    fun markDailyPromptSeen(dayKey: String) {
        prefs.edit().putString(KEY_DAILY_PROMPT_DAY, dayKey).apply()
    }

    /** Counts this opening of the app and returns how many openings there have been since the last spending tip. */
    fun recordOpening(): Int {
        val openings = prefs.getInt(KEY_OPENINGS_SINCE_TIP, 0) + 1
        prefs.edit().putInt(KEY_OPENINGS_SINCE_TIP, openings).apply()
        return openings
    }

    fun markTipShown() {
        prefs.edit().putInt(KEY_OPENINGS_SINCE_TIP, 0).apply()
    }

    private companion object {
        const val KEY_NAME = "name"
        const val KEY_EMAIL = "email"
        const val KEY_INCOME = "monthly_income_minor"
        const val KEY_BACKUP_INTERVAL = "backup_interval"
        const val KEY_BACKUP_FOLDER_URI = "backup_folder_uri"
        const val KEY_BACKUP_LOCATION_URI = "backup_location_uri"
        const val KEY_BACKUP_NOTICE_DISMISSED = "backup_notice_dismissed"
        const val KEY_TUTORIAL_SEEN = "tutorial_seen"
        const val KEY_DAILY_PROMPT_DAY = "daily_prompt_day"
        const val KEY_OPENINGS_SINCE_TIP = "openings_since_tip"
    }
}
