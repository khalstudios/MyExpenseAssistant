package com.khaltech.expenseassistant.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * The one connection to Play Billing.
 *
 * Everything it knows is a cache of what Play said last. There is no backend to verify against, so
 * the rule throughout is: a successful answer from Play replaces what we believed, and anything
 * else — no network, Play Store missing, service disconnected — leaves it untouched.
 */
class BillingManager(
    context: Context,
    private val entitlements: EntitlementStore,
) : PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _isPro = MutableStateFlow(entitlements.isPro())

    /** Seeded from the cache, so the UI is never briefly wrong while Play is being asked. */
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    private val _offers = MutableStateFlow<List<ProOffer>>(emptyList())

    /** Empty until Play answers, which is why the paywall has to cope with having no prices yet. */
    val offers: StateFlow<List<ProOffer>> = _offers.asStateFlow()

    private val _events = MutableSharedFlow<PurchaseEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<PurchaseEvent> = _events.asSharedFlow()

    private val client = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
        )
        .enableAutoServiceReconnection()
        .build()

    @Volatile private var connecting = false

    /**
     * Asks Play what this user owns and what the products cost. Safe to call often — on every
     * resume, for instance — because a purchase made on another device only shows up when asked.
     */
    fun refresh() {
        connect {
            scope.launch {
                // Selling Pro must never be able to take down an app whose actual job is recording
                // what someone spent. If Play throws, the cached entitlement stands and the paywall
                // simply has no prices to show.
                runCatching {
                    refreshEntitlements()
                    refreshOffers()
                }
            }
        }
    }

    fun purchase(activity: Activity, offer: ProOffer) {
        val product = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(offer.details)
            .apply { offer.offerToken?.let(::setOfferToken) }
            .build()
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(product))
            .build()
        val result = client.launchBillingFlow(activity, params)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _events.tryEmit(PurchaseEvent.Failed(result.readableMessage()))
        }
    }

    /** Debug builds only; see [EntitlementStore.debugOverride]. Takes effect immediately. */
    fun setDebugOverride(forcePro: Boolean?) {
        entitlements.setDebugOverride(forcePro)
        _isPro.value = entitlements.isPro()
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> scope.launch {
                val ours = purchases.orEmpty().filter(::isOurs)
                ours.forEach { acknowledge(it) }
                if (ours.any { it.isActive() }) {
                    grant()
                    _events.tryEmit(PurchaseEvent.Purchased)
                }
            }

            BillingClient.BillingResponseCode.USER_CANCELED ->
                _events.tryEmit(PurchaseEvent.Cancelled)

            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                // Bought on another device, or a previous purchase never made it back to us.
                _events.tryEmit(PurchaseEvent.AlreadyOwned)
                refresh()
            }

            else -> _events.tryEmit(PurchaseEvent.Failed(result.readableMessage()))
        }
    }

    private fun connect(onReady: () -> Unit) {
        if (client.isReady) {
            onReady()
            return
        }
        if (connecting) return
        connecting = true
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                connecting = false
                if (result.responseCode == BillingClient.BillingResponseCode.OK) onReady()
            }

            // Auto reconnection handles getting back. Nothing to undo here, because the cached
            // entitlement stays valid while Play is away.
            override fun onBillingServiceDisconnected() {
                connecting = false
            }
        })
    }

    private suspend fun refreshEntitlements() {
        val oneTime = queryPurchases(BillingClient.ProductType.INAPP)
        val subscriptions = queryPurchases(BillingClient.ProductType.SUBS)

        // A null answer means Play could not tell us, not that nothing is owned. Revoking here
        // would lock a paying user out of their own app the first time they opened it offline.
        if (oneTime == null || subscriptions == null) return

        val ours = (oneTime + subscriptions).filter(::isOurs)
        ours.forEach { acknowledge(it) }
        entitlements.setPurchased(ours.any { it.isActive() })
        _isPro.value = entitlements.isPro()
    }

    private suspend fun queryPurchases(type: String): List<Purchase>? =
        suspendCancellableCoroutine { continuation ->
            val params = QueryPurchasesParams.newBuilder().setProductType(type).build()
            client.queryPurchasesAsync(params) { result, purchases ->
                continuation.resume(
                    purchases.takeIf { result.responseCode == BillingClient.BillingResponseCode.OK },
                )
            }
        }

    /**
     * Play refunds a purchase that is not acknowledged within three days, so this runs over every
     * purchase we see, not only freshly bought ones.
     */
    private suspend fun acknowledge(purchase: Purchase) {
        if (!purchase.isActive() || purchase.isAcknowledged) return
        suspendCancellableCoroutine { continuation ->
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            client.acknowledgePurchase(params) { continuation.resume(Unit) }
        }
    }

    private suspend fun refreshOffers() {
        // Play rejects outright a query whose products are not all the same type, so the one-time
        // purchase and the subscription have to be asked for in separate calls.
        val details = ProProduct.entries
            .groupBy { it.playType }
            .flatMap { (type, products) -> queryProductDetails(type, products) }

        // Keep the prices we already had rather than emptying the paywall on a failed query.
        if (details.isNotEmpty()) {
            _offers.value = details.mapNotNull { it.toOffer() }.sortedBy { it.product.ordinal }
        }
    }

    private suspend fun queryProductDetails(
        type: String,
        products: List<ProProduct>,
    ): List<ProductDetails> {
        val queried = products.map { product ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(product.productId)
                .setProductType(type)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder().setProductList(queried).build()
        return suspendCancellableCoroutine { continuation ->
            client.queryProductDetailsAsync(params) { result, queryResult ->
                continuation.resume(
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        queryResult.productDetailsList
                    } else {
                        emptyList()
                    },
                )
            }
        }
    }

    private fun grant() {
        entitlements.setPurchased(true)
        _isPro.value = true
    }

    private fun isOurs(purchase: Purchase): Boolean =
        purchase.products.any { ProProduct.forId(it) != null }

    /**
     * Pending purchases — cash paid at a counter, for instance — are deliberately not treated as
     * owned. They become owned when Play tells us they completed.
     */
    private fun Purchase.isActive(): Boolean = purchaseState == Purchase.PurchaseState.PURCHASED

    private fun ProductDetails.toOffer(): ProOffer? {
        val product = ProProduct.forId(productId) ?: return null
        return when (product.playType) {
            BillingClient.ProductType.SUBS -> {
                val offer = subscriptionOfferDetails?.firstOrNull() ?: return null
                // The last phase is the standing price; earlier ones are intro or free periods.
                val price = offer.pricingPhases.pricingPhaseList.lastOrNull()?.formattedPrice
                    ?: return null
                ProOffer(product, price, this, offer.offerToken)
            }

            else -> {
                // Billing 8 added per-offer pricing for one-time products; a listing without one
                // still answers through the single-offer accessor.
                val listed = oneTimePurchaseOfferDetailsList?.firstOrNull()
                if (listed != null) {
                    ProOffer(product, listed.formattedPrice, this, listed.offerToken)
                } else {
                    val single = oneTimePurchaseOfferDetails ?: return null
                    ProOffer(product, single.formattedPrice, this, null)
                }
            }
        }
    }

    private fun BillingResult.readableMessage(): String =
        debugMessage.takeIf { it.isNotBlank() } ?: "Something went wrong with the purchase."
}
