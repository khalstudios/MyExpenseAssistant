package com.khaltech.expenseassistant.billing

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails

/**
 * What Pro is sold as. Both products unlock exactly the same features; the only difference is how
 * the user would rather pay for them. The ids must match the ones created in the Play Console.
 */
enum class ProProduct(val productId: String, val playType: String) {
    LIFETIME("pro_lifetime", BillingClient.ProductType.INAPP),
    YEARLY("pro_yearly", BillingClient.ProductType.SUBS),
    ;

    companion object {
        fun forId(productId: String): ProProduct? = entries.firstOrNull { it.productId == productId }
    }
}

/**
 * One product as Play priced it for this user. [formattedPrice] already carries the right currency
 * and locale, so the paywall never hard-codes an amount.
 */
data class ProOffer(
    val product: ProProduct,
    val formattedPrice: String,
    val details: ProductDetails,
    /** Required for subscriptions, and for one-time products that Play returns an offer for. */
    val offerToken: String?,
)

/** Something worth telling the user about after they came back from the Play sheet. */
sealed interface PurchaseEvent {
    data object Purchased : PurchaseEvent
    data object Cancelled : PurchaseEvent
    data object AlreadyOwned : PurchaseEvent
    data class Failed(val message: String) : PurchaseEvent
}
