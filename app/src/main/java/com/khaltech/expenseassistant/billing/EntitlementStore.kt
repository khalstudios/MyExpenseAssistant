package com.khaltech.expenseassistant.billing

import android.content.Context
import com.khaltech.expenseassistant.BuildConfig

/**
 * Remembers whether this install has Pro.
 *
 * The app has no server, so Play is the only authority on what was bought. Play is not always
 * reachable, though, and someone who paid must not lose their features on a train, so the answer is
 * cached here and only ever replaced by a *successful* Play query. A failed query leaves the cache
 * alone.
 */
class EntitlementStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("pro-entitlement", Context.MODE_PRIVATE)

    private val firstInstallTime: Long = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
    }.getOrDefault(Long.MAX_VALUE)

    fun isPro(): Boolean = debugOverride() ?: (isGrandfathered() || prefs.getBoolean(KEY_PURCHASED, false))

    /**
     * Debug builds only: Pro forced on or off for testing either tier on a real device, or null to
     * follow what was actually earned or bought. Always null in a release build, whatever is stored.
     */
    fun debugOverride(): Boolean? =
        if (BuildConfig.DEBUG && prefs.contains(KEY_DEBUG_OVERRIDE)) prefs.getBoolean(KEY_DEBUG_OVERRIDE, false)
        else null

    fun setDebugOverride(forcePro: Boolean?) {
        if (!BuildConfig.DEBUG) return
        prefs.edit().apply {
            if (forcePro == null) remove(KEY_DEBUG_OVERRIDE) else putBoolean(KEY_DEBUG_OVERRIDE, forcePro)
        }.apply()
    }

    /**
     * True for anyone who installed the app before Pro existed. They already had these features for
     * free and taking them away would be a betrayal, so the app keeps its side of that bargain.
     *
     * The verdict is worked out once and then stored, so a later Play Store reinstall (which resets
     * `firstInstallTime`) cannot quietly revoke it on a device that had already earned it.
     */
    fun isGrandfathered(): Boolean {
        if (prefs.contains(KEY_GRANDFATHERED)) return prefs.getBoolean(KEY_GRANDFATHERED, false)
        val earned = firstInstallTime < PRO_RELEASE_MILLIS
        prefs.edit().putBoolean(KEY_GRANDFATHERED, earned).apply()
        return earned
    }

    /** True once the "you keep everything, on us" note has been shown to a grandfathered user. */
    fun isGrandfatherNoticeSeen(): Boolean = prefs.getBoolean(KEY_GRANDFATHER_NOTICE, false)

    fun markGrandfatherNoticeSeen() {
        prefs.edit().putBoolean(KEY_GRANDFATHER_NOTICE, true).apply()
    }

    /** Only ever called with the result of a Play query that actually succeeded. */
    fun setPurchased(purchased: Boolean) {
        prefs.edit().putBoolean(KEY_PURCHASED, purchased).apply()
    }

    private companion object {
        const val KEY_PURCHASED = "purchased"
        const val KEY_GRANDFATHERED = "grandfathered"
        const val KEY_GRANDFATHER_NOTICE = "grandfather_notice_seen"
        const val KEY_DEBUG_OVERRIDE = "debug_override"

        /**
         * 2026-09-22T07:53:37Z, when 2.0.0 (the first build containing the paywall) was cut:
         * every install older than it keeps Pro for free.
         */
        const val PRO_RELEASE_MILLIS = 1_790_063_617_000L
    }
}
