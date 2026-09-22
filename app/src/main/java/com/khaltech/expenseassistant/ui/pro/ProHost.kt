package com.khaltech.expenseassistant.ui.pro

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.khaltech.expenseassistant.billing.PurchaseEvent
import com.khaltech.expenseassistant.data.backup.AutoBackupScheduler
import com.khaltech.expenseassistant.di.ServiceLocator

/**
 * Owns everything to do with buying Pro, and hands the answer down through [LocalPro].
 *
 * This sits above the app's own navigation so that screens pushed over the tabs — a category's
 * transactions, for instance — are gated by the same value as the tabs themselves.
 */
@Composable
fun ProHost(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val billing = remember { ServiceLocator.billing(context) }
    val entitlements = remember { ServiceLocator.entitlementStore(context) }
    val isPro by billing.isPro.collectAsStateWithLifecycle()
    val offers by billing.offers.collectAsStateWithLifecycle()

    var showPaywall by remember { mutableStateOf(false) }
    var pitch by remember { mutableStateOf<ProPitch?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var showGrandfatherNotice by remember {
        mutableStateOf(entitlements.isGrandfathered() && !entitlements.isGrandfatherNoticeSeen())
    }

    // A purchase can happen on another device, or be refunded, so the answer is re-asked every
    // time the app comes forward rather than only once at startup.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) billing.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Stop automatic backups as soon as Play says Pro has lapsed, rather than leaving a schedule
    // enqueued until its next run notices. The worker checks again before it writes anything, so
    // this is the prompt path, not the only one.
    LaunchedEffect(isPro) {
        if (!isPro) AutoBackupScheduler.enforceEntitlement(context)
    }

    LaunchedEffect(billing) {
        billing.events.collect { event ->
            when (event) {
                PurchaseEvent.Purchased -> {
                    showPaywall = false
                    message = "You're on Pro. Thank you for supporting the app."
                }

                PurchaseEvent.AlreadyOwned -> {
                    showPaywall = false
                    message = "This Google account already owns Pro, so it's unlocked again here."
                }

                PurchaseEvent.Restored -> {
                    showPaywall = false
                    message = "Your Pro purchase has been restored."
                }

                PurchaseEvent.NothingToRestore -> message =
                    "Google Play found no Pro purchase on this account. If you bought Pro with a " +
                    "different Google account, switch to it in the Play Store and try again."

                PurchaseEvent.RestoreFailed -> message =
                    "Couldn't reach Google Play. Check your connection and try again."

                // Backing out of the Play sheet is a normal thing to do, not an error.
                PurchaseEvent.Cancelled -> Unit

                is PurchaseEvent.Failed -> message = event.message
            }
        }
    }

    CompositionLocalProvider(
        LocalPro provides ProStatus(
            isPro = isPro,
            onUpgrade = {
                pitch = null
                showPaywall = true
            },
            onUpgradeFor = {
                pitch = it
                showPaywall = true
            },
        ),
    ) {
        content()
    }

    if (showPaywall) {
        val activity = context.findActivity()
        ProPaywallSheet(
            pitch = pitch,
            offers = offers,
            onBuy = { offer -> activity?.let { billing.purchase(it, offer) } },
            onRetry = { billing.refresh() },
            onRestore = { billing.restore() },
            onDismiss = { showPaywall = false },
        )
    }

    message?.let { text ->
        AlertDialog(
            onDismissRequest = { message = null },
            confirmButton = { TextButton(onClick = { message = null }) { Text("OK") } },
            text = { Text(text) },
        )
    }

    if (showGrandfatherNotice) {
        AlertDialog(
            onDismissRequest = {
                showGrandfatherNotice = false
                entitlements.markGrandfatherNoticeSeen()
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showGrandfatherNotice = false
                        entitlements.markGrandfatherNoticeSeen()
                    },
                ) { Text("Thanks") }
            },
            title = { Text("Pro is yours, free") },
            text = {
                Text(
                    "Some features are part of Pro from this update onwards. You were here before " +
                        "that, so you keep all of them at no cost — nothing to buy, nothing to do.",
                )
            },
        )
    }
}

/** Compose hands out a themed wrapper, not the Activity that Play Billing needs to show its sheet. */
private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
