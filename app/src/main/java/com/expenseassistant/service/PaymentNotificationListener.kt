package com.expenseassistant.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.expenseassistant.data.model.CaptureSource
import com.expenseassistant.di.ServiceLocator
import com.expenseassistant.parser.PaymentApps
import com.expenseassistant.parser.PaymentTextParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PaymentNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        if (!PaymentApps.isSupported(packageName)) return
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val text = sbn.notification.extractText()
        if (text.isBlank()) return

        scope.launch {
            runCatching {
                val payment = PaymentTextParser.parse(text, packageName, sbn.postTime) ?: return@runCatching
                val id = ServiceLocator.repository(applicationContext)
                    .ingest(payment, CaptureSource.NOTIFICATION)
                Log.d(TAG, "Recorded transaction $id from $packageName")
            }.onFailure { Log.e(TAG, "Failed to handle notification from $packageName", it) }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun Notification.extractText(): String {
        val e = extras
        val parts = listOfNotNull(
            e.getCharSequence(Notification.EXTRA_TITLE),
            e.getCharSequence(Notification.EXTRA_TEXT),
            e.getCharSequence(Notification.EXTRA_BIG_TEXT),
            e.getCharSequence(Notification.EXTRA_SUB_TEXT),
            e.getCharSequence(Notification.EXTRA_SUMMARY_TEXT),
            e.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.joinToString(" "),
        )
        return parts.map { it.toString().trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString(" — ")
    }

    private companion object {
        const val TAG = "PaymentNotifListener"
    }
}
