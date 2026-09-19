package com.khaltech.expenseassistant.billing

import android.content.Context

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

    fun isPro(): Boolean = isGrandfathered() || prefs.getBoolean(KEY_PURCHASED, false)

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

        /**
         * 2026-10-01T00:00:00Z. Set this to the moment the first build containing the paywall
         * reaches production: every install older than it keeps Pro for free.
         */
        const val PRO_RELEASE_MILLIS = 1_790_812_800_000L
    }
}
