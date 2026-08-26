package com.expenseassistant.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.expenseassistant.data.model.CaptureSource
import com.expenseassistant.di.ServiceLocator
import com.expenseassistant.parser.PaymentApps
import com.expenseassistant.parser.PaymentTextParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Fallback capture path: some UPI apps show a "Payment successful" screen without
 * posting a notification. We scan only the visible text of whitelisted payment apps
 * and only when the screen looks like a success confirmation.
 */
class PaymentScreenAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastHandledText: String? = null
    private var lastHandledAt = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        if (packageName !in PaymentApps.screenScanTargets) return

        val root = rootInActiveWindow ?: return
        val screenText = runCatching { root.collectText() }.getOrNull()?.takeIf { it.isNotBlank() } ?: return
        if (!screenText.lowercase().let { it.contains("success") || it.contains("paid") || it.contains("completed") }) return

        val now = System.currentTimeMillis()
        if (screenText == lastHandledText && now - lastHandledAt < REPEAT_SUPPRESSION_MS) return
        lastHandledText = screenText
        lastHandledAt = now

        scope.launch {
            runCatching {
                val payment = PaymentTextParser.parse(screenText, packageName, now) ?: return@runCatching
                val id = ServiceLocator.repository(applicationContext)
                    .ingest(payment, CaptureSource.SCREEN)
                if (id != null) Log.d(TAG, "Recorded transaction $id from $packageName screen")
            }.onFailure { Log.e(TAG, "Failed to handle screen from $packageName", it) }
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun AccessibilityNodeInfo.collectText(depth: Int = 0): String {
        if (depth > MAX_DEPTH) return ""
        val builder = StringBuilder()
        text?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { builder.append(it).append(' ') }
        contentDescription?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { builder.append(it).append(' ') }
        for (i in 0 until childCount) {
            val child = getChild(i) ?: continue
            builder.append(child.collectText(depth + 1))
            child.recycle()
        }
        return builder.toString()
    }

    private companion object {
        const val TAG = "PaymentScreenService"
        const val MAX_DEPTH = 40
        const val REPEAT_SUPPRESSION_MS = 10_000L
    }
}
