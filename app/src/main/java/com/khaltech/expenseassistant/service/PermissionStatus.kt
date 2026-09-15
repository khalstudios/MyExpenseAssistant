package com.khaltech.expenseassistant.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
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

    fun notificationAccessIntent(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
