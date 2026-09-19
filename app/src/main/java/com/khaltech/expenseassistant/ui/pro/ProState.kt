package com.khaltech.expenseassistant.ui.pro

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Whether this user has Pro, and how to ask them to buy it.
 *
 * Gating happens deep inside screens — a single card on Insights, one tap target on a chart — and
 * threading a boolean and a callback down through every composable in between would bury the real
 * arguments. [MainActivity] provides this once at the root instead.
 */
@Immutable
data class ProStatus(
    val isPro: Boolean,
    val onUpgrade: () -> Unit,
)

/**
 * Defaults to locked. If a provider is ever missed, the failure shows up as a paywall that will not
 * go away, which someone will notice, rather than as features quietly given away for free.
 */
val LocalPro = staticCompositionLocalOf { ProStatus(isPro = false, onUpgrade = {}) }

/** The pitch, kept in one place so the paywall and the store listing cannot drift apart. */
val ProFeatures: List<String> = listOf(
    "See every transaction behind a category",
    "Spending analytics for your tags",
    "Spot recurring payments and forgotten subscriptions",
    "Trends and comparisons across any period",
    "Automatic backups on your schedule",
)
