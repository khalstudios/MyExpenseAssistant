package com.khaltech.expenseassistant.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.provider.Telephony
import androidx.core.content.ContextCompat

object PermissionStatus {

    fun isContactsAccessGranted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    fun isNotificationAccessGranted(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        ) ?: return false
        val component = ComponentName(context, PaymentNotificationListener::class.java)
        return enabled.split(':').any { entry ->
            ComponentName.unflattenFromString(entry)?.equals(component) == true
        }
    }

    /** True on Android 13+ when the app may not yet post its own payment and budget alerts. */
    fun needsPostNotificationsPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED

    fun notificationAccessIntent(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** The phone's default SMS app, or null on devices without one. Needs no permission. */
    fun defaultSmsPackage(context: Context): String? =
        runCatching { Telephony.Sms.getDefaultSmsPackage(context) }.getOrNull()

    /** The notification settings of the default SMS app, falling back to the main Settings screen. */
    fun smsNotificationSettingsIntent(context: Context): Intent {
        val smsPackage = defaultSmsPackage(context)
        val intent = if (smsPackage != null) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, smsPackage)
        } else {
            Intent(Settings.ACTION_SETTINGS)
        }
        return intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
