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
    "Categories of your own, named and coloured how you like",
    "As many tags of your own as you like, all charted",
    "Spot recurring payments, and add your own",
    "Automatic backups on your schedule",
)

/**
 * How many tags of their own a free user may create.
 *
 * Tagging is free — a handful of tags is enough to learn what they are for and to keep using the
 * app happily. Somebody running a dozen of them is organising their spending in earnest, and that
 * is the point at which Pro is worth asking for. Tags already in use are never taken away, so this
 * only ever stops the next new one from being created.
 */
const val FreeTagLimit = 5
