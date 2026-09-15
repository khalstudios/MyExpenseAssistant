package com.khaltech.expenseassistant.notify

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.khaltech.expenseassistant.R
import com.khaltech.expenseassistant.data.model.Direction
import com.khaltech.expenseassistant.data.model.TransactionEntity
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Pops up a brief alert each time a payment is captured automatically, then removes it: the
 * transaction is already in the list, so the alert has nothing left to do once it has been seen.
 */
class TransactionNotifier(private val context: Context) {

    private val currency = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        currency = Currency.getInstance("INR")
        minimumFractionDigits = 0
        maximumFractionDigits = 2
    }

    fun onTransactionRecorded(transaction: TransactionEntity) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        val amount = currency.format(transaction.amountMinor / 100.0)
        val headline = when (transaction.direction) {
            Direction.DEBIT -> "$TITLE: $amount at ${transaction.merchant}"
            Direction.CREDIT -> "$TITLE: $amount from ${transaction.merchant}"
        }
        val category = transaction.customCategoryName ?: transaction.category.displayName
        val account = listOfNotNull(
            transaction.bankName,
            transaction.accountLast4?.let { "••$it" },
        ).joinToString(" ").takeIf { it.isNotEmpty() }
        val details = listOfNotNull(category, account).joinToString(" · ")

        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pendingIntent = launch?.let {
            PendingIntent.getActivity(
                context,
                0,
                it,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }

        // The lock screen shows only that something was recorded, never the amount or payee.
        val appName = context.getString(R.string.app_name)
        val publicVersion = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(appName)
            .setContentText(TITLE)
            .build()

        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            // Collapsed: the app name, then one line saying what was recorded. Expanded adds the details.
            .setContentTitle(appName)
            .setContentText(headline)
            .setStyle(Notification.BigTextStyle().bigText("$headline\n$details"))
            .setCategory(Notification.CATEGORY_STATUS)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .setAutoCancel(true)
            .setTimeoutAfter(DISPLAY_MILLIS)
            .apply { pendingIntent?.let(::setContentIntent) }
            .build()

        // One notification per transaction, so payments captured together each get their own alert.
        runCatching { manager.notify(NOTIFICATION_TAG, transaction.id.toInt(), notification) }
    }

    companion object {
        // A channel's sound is fixed once created, so going silent needed a new id; the old one is deleted.
        const val CHANNEL_ID = "transaction-recorded-silent"
        private const val RETIRED_CHANNEL_ID = "transaction-recorded"
        private const val NOTIFICATION_TAG = "transaction-recorded"
        private const val TITLE = "Transaction recorded"
        private const val DISPLAY_MILLIS = 15_000L

        /**
         * High importance is what makes Android show it as a heads-up alert; with no sound or
         * vibration the alert pops up silently.
         */
        fun createChannel(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.deleteNotificationChannel(RETIRED_CHANNEL_ID)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Recorded transactions",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "A brief, silent alert each time a payment is recorded automatically"
                setSound(null, null)
                enableVibration(false)
            }
            manager.createNotificationChannel(channel)
        }
    }
}
