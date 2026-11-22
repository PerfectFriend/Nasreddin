package com.example.monetization

import android.app.Activity
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

object SkuCatalog {
    const val SKU_PRO_LIFETIME = "hodja_pro_lifetime"
    const val SKU_SERIES_VOLUME1 = "hodja_series_volume1"
    val ALL_IN_APP_SKUS = listOf(SKU_PRO_LIFETIME, SKU_SERIES_VOLUME1)
}

data class StoreProductPrices(
    val proFormattedPrice: String? = null,
    val seriesFormattedPrice: String? = null
)

sealed class BillingEvent {
    data class PurchaseGranted(val sku: String) : BillingEvent()
    data class RestoredPurchases(val grantedSkus: Set<String>, val userInitiated: Boolean) : BillingEvent()
    data object PurchasePending : BillingEvent()
    data object UserCancelled : BillingEvent()
    data class Error(val debugMessage: String?) : BillingEvent()
}

interface BillingGateway {
    val productPrices: StateFlow<StoreProductPrices>
    val isReady: StateFlow<Boolean>
    val events: SharedFlow<BillingEvent>

    fun connect()
    fun queryOwnedPurchases(userInitiated: Boolean)
    fun launchPurchase(activity: Activity?, sku: String)
    fun destroy()
}

sealed class AdShowResult {
    data object RewardEarned : AdShowResult()
    data object ClosedWithoutReward : AdShowResult()
    data class FailedToShow(val reason: String?) : AdShowResult()
}

interface AdsGateway {
    val isRewardedReady: StateFlow<Boolean>
    val isInterstitialReady: StateFlow<Boolean>

    fun initialize()
    fun preloadRewardedAd()
    fun preloadInterstitialAd()
    fun showRewardedAd(activity: Activity?, onResult: (AdShowResult) -> Unit)
    fun showInterstitialAd(activity: Activity?, onClosed: () -> Unit)
}
