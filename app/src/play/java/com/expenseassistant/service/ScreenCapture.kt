package com.expenseassistant.service

import android.content.ComponentName
import android.content.Context

/**
 * Play build: no on-screen capture. Google Play restricts the Accessibility API to
 * accessibility purposes, so the service, its manifest entry and its configuration are
 * all confined to the direct-distribution source set.
 */
object ScreenCapture {
    const val AVAILABLE = false

    @Suppress("UNUSED_PARAMETER")
    fun component(context: Context): ComponentName? = null
}
