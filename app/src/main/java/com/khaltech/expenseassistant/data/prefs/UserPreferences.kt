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

    fun isTutorialSeen(): Boolean = prefs.getBoolean(KEY_TUTORIAL_SEEN, false)

    fun markTutorialSeen() {
        prefs.edit().putBoolean(KEY_TUTORIAL_SEEN, true).apply()
    }

    /**
     * True when the automatic-backup reminder should show: never before backups are on, never on the
     * first opening (which starts the clock), then once every [BackupReminderIntervalMillis].
     */
    fun isBackupReminderDue(now: Long): Boolean {
        if (autoBackupSettings() != null) return false
        val last = prefs.getLong(KEY_BACKUP_REMINDER_AT, 0)
        if (last == 0L) {
            prefs.edit().putLong(KEY_BACKUP_REMINDER_AT, now).apply()
            return false
        }
        return now - last >= BackupReminderIntervalMillis
    }

    fun markBackupReminderShown(now: Long) {
        prefs.edit().putLong(KEY_BACKUP_REMINDER_AT, now).apply()
    }

    /** When notification capture was first seen working, so the SMS check gives alerts time to arrive. */
    fun markCaptureConnected(now: Long) {
        if (!prefs.contains(KEY_CAPTURE_CONNECTED_AT)) prefs.edit().putLong(KEY_CAPTURE_CONNECTED_AT, now).apply()
    }

    /** True once any notification from an SMS app has reached the listener. Only the fact is kept, never the message. */
    fun isSmsNotificationSeen(): Boolean = prefs.getBoolean(KEY_SMS_NOTIFICATION_SEEN, false)

    fun markSmsNotificationSeen() {
        if (!isSmsNotificationSeen()) prefs.edit().putBoolean(KEY_SMS_NOTIFICATION_SEEN, true).apply()
    }

    fun isSmsPromptSeen(): Boolean = prefs.getBoolean(KEY_SMS_PROMPT_SEEN, false)

    fun markSmsPromptSeen() {
        prefs.edit().putBoolean(KEY_SMS_PROMPT_SEEN, true).apply()
    }

    fun dismissSmsNotice() {
        prefs.edit().putBoolean(KEY_SMS_NOTICE_DISMISSED, true).apply()
    }

    /**
     * True when capture has been running for [SmsCheckDelayMillis] without a single SMS-app
     * notification, which usually means the SMS app's notifications are turned off.
     */
    fun isSmsCheckDue(now: Long): Boolean {
        if (isSmsNotificationSeen() || prefs.getBoolean(KEY_SMS_NOTICE_DISMISSED, false)) return false
        val connectedAt = prefs.getLong(KEY_CAPTURE_CONNECTED_AT, 0)
        return connectedAt > 0 && now - connectedAt >= SmsCheckDelayMillis
    }

    /** Queues one-off hints, by key, to be shown once each. Only a new install queues them. */
    fun queueHints(keys: Collection<String>) {
        prefs.edit().putStringSet(KEY_PENDING_HINTS, pendingHints() + keys).apply()
    }

    fun isHintPending(key: String): Boolean = key in pendingHints()

    fun markHintShown(key: String) {
        prefs.edit().putStringSet(KEY_PENDING_HINTS, pendingHints() - key).apply()
    }

    private fun pendingHints(): Set<String> = prefs.getStringSet(KEY_PENDING_HINTS, null).orEmpty().toSet()

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
        const val KEY_TUTORIAL_SEEN = "tutorial_seen"
        const val KEY_OPENINGS_SINCE_TIP = "openings_since_tip"
        const val KEY_CAPTURE_CONNECTED_AT = "capture_connected_at"
        const val KEY_SMS_NOTIFICATION_SEEN = "sms_notification_seen"
        const val KEY_SMS_PROMPT_SEEN = "sms_prompt_seen"
        const val KEY_SMS_NOTICE_DISMISSED = "sms_notice_dismissed"
        const val KEY_PENDING_HINTS = "pending_hints"
        const val KEY_BACKUP_REMINDER_AT = "backup_reminder_at"
        const val BackupReminderIntervalMillis = 3 * 24 * 60 * 60 * 1000L
        const val SmsCheckDelayMillis = 3 * 24 * 60 * 60 * 1000L
    }
}
