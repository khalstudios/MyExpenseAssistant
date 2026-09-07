package com.expenseassistant.service

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
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
    private var lastScanAt = 0L
    private var lastCapturedText: String? = null
    private var lastCapturedAt = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        if (packageName !in PaymentApps.screenScanTargets) return

        // Content-changed events fire on every scroll frame; never scan more often than this.
        val uptime = SystemClock.elapsedRealtime()
        if (uptime - lastScanAt < SCAN_THROTTLE_MS) return

        val root = rootInActiveWindow ?: return

        // Cheap gate before any tree walking: only a confirmation screen says "success".
        if (!hasSuccessMarker(root)) return
        lastScanAt = uptime

        val screenText = runCatching { root.collectText() }.getOrNull()?.takeIf { it.isNotBlank() } ?: return
        if (PaymentTextParser.looksLikeHistoryScreen(screenText)) {
            Log.d(TAG, "Ignoring history-like screen in $packageName")
            return
        }

        val now = System.currentTimeMillis()
        if (screenText == lastCapturedText && now - lastCapturedAt < REPEAT_SUPPRESSION_MS) return
        lastCapturedText = screenText
        lastCapturedAt = now

        scope.launch {
            runCatching {
                val payment = PaymentTextParser.parse(
                    text = screenText,
                    sourcePackage = packageName,
                    occurredAt = now,
                    requireStrongSuccess = true,
                ) ?: return@runCatching
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

    private fun hasSuccessMarker(root: AccessibilityNodeInfo): Boolean = SUCCESS_MARKERS.any { marker ->
        runCatching { root.findAccessibilityNodeInfosByText(marker) }.getOrNull()?.isNotEmpty() == true
    }

    private fun AccessibilityNodeInfo.collectText(): String {
        val builder = StringBuilder()
        appendText(builder, depth = 0, visited = intArrayOf(0))
        return builder.toString()
    }

    private fun AccessibilityNodeInfo.appendText(builder: StringBuilder, depth: Int, visited: IntArray) {
        if (depth > MAX_DEPTH || visited[0] >= MAX_NODES) return
        visited[0]++
        text?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { builder.append(it).append(' ') }
        contentDescription?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { builder.append(it).append(' ') }
        for (i in 0 until childCount) {
            if (visited[0] >= MAX_NODES) return
            getChild(i)?.appendText(builder, depth + 1, visited)
        }
    }

    private companion object {
        const val TAG = "PaymentScreenService"
        const val MAX_DEPTH = 12
        const val MAX_NODES = 150
        const val SCAN_THROTTLE_MS = 1_500L
        const val REPEAT_SUPPRESSION_MS = 30_000L
        val SUCCESS_MARKERS = listOf("Successful", "successful", "Success")
    }
}
