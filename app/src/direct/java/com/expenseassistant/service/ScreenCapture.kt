package com.expenseassistant.service

import android.content.ComponentName
import android.content.Context

/**
 * Direct-distribution build: on-screen capture is available, backed by
 * [PaymentScreenAccessibilityService].
 */
object ScreenCapture {
    const val AVAILABLE = true

    fun component(context: Context): ComponentName =
        ComponentName(context, PaymentScreenAccessibilityService::class.java)
}
