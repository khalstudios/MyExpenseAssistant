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
    /** The paywall led by what the user just reached for, rather than the general pitch. */
    val onUpgradeFor: (ProPitch) -> Unit = { onUpgrade() },
)

/**
 * A headline for the paywall naming the thing that opened it. Someone who tapped a locked category
 * wants to hear about categories first; the full feature list still follows underneath.
 */
@Immutable
data class ProPitch(val headline: String, val detail: String)

object ProPitches {
    val CustomCategories = ProPitch(
        headline = "Make every category yours",
        detail = "You've used your $FreeCustomCategoryLimit free categories. Pro lets you create as " +
            "many as you like, each with its own name, colour and icon.",
    )

    fun extendedCategory(name: String) = ProPitch(
        headline = "File it under $name",
        detail = "$name and the other Pro categories, plus as many of your own as you like.",
    )
}

/**
 * Defaults to locked. If a provider is ever missed, the failure shows up as a paywall that will not
 * go away, which someone will notice, rather than as features quietly given away for free.
 */
val LocalPro = staticCompositionLocalOf { ProStatus(isPro = false, onUpgrade = {}) }

/** The pitch, kept in one place so the paywall and the store listing cannot drift apart. */
val ProFeatures: List<String> = listOf(
    "Unlimited categories of your own, plus House, Vehicle, Hobbies and more",
    "House and vehicle payments filed automatically — garages, plumbers, servicing",
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

/**
 * How many categories of their own a free user may create.
 *
 * The same reasoning as [FreeTagLimit]: none at all means a free user never finds out what a
 * category of their own is like, so the lock sells nothing. Two is enough to start filing under
 * them in earnest, which is when a third is worth paying for. Existing ones are never taken away.
 */
const val FreeCustomCategoryLimit = 2
