package com.example.monetization

import android.app.Activity
import android.content.Context
import android.util.Log
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
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Production Google Play Billing 9.x manager for one-time non-consumable unlocks
 * (`hodja_pro_lifetime` and `hodja_series_volume1`), handling acknowledgment,
 * startup/resume restore, pending transactions, and ITEM_ALREADY_OWNED reconciliation.
 */
class PlayBillingManager(context: Context) : BillingGateway {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _productPrices = MutableStateFlow(StoreProductPrices())
    override val productPrices: StateFlow<StoreProductPrices> = _productPrices.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    override val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _events = MutableSharedFlow<BillingEvent>(extraBufferCapacity = 16)
    override val events: SharedFlow<BillingEvent> = _events.asSharedFlow()

    @Volatile
    private var cachedProductDetails: Map<String, ProductDetails> = emptyMap()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (!purchases.isNullOrEmpty()) {
                    scope.launch { processCompletedPurchases(purchases, fromRestore = false, userInitiatedRestore = false) }
                } else {
                    _events.tryEmit(BillingEvent.Error("Empty purchase payload returned"))
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _events.tryEmit(BillingEvent.UserCancelled)
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                queryOwnedPurchases(userInitiated = false)
            }
            else -> {
                Log.w(TAG, "onPurchasesUpdated error ${billingResult.responseCode}: ${billingResult.debugMessage}")
                _events.tryEmit(BillingEvent.Error(billingResult.debugMessage.ifBlank { null }))
            }
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .enableAutoServiceReconnection()
        .build()

    override fun connect() {
        if (_isReady.value && billingClient.isReady) {
            scope.launch { refreshCatalogAndRestoreSilently() }
            return
        }
        try {
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        _isReady.value = true
                        scope.launch { refreshCatalogAndRestoreSilently() }
                    } else {
                        _isReady.value = false
                        Log.w(TAG, "Billing setup failed (${billingResult.responseCode}): ${billingResult.debugMessage}")
                    }
                }

                override fun onBillingServiceDisconnected() {
                    _isReady.value = false
                }
            })
        } catch (e: Exception) {
            _isReady.value = false
            Log.e(TAG, "Failed to start BillingClient connection", e)
        }
    }

    private suspend fun refreshCatalogAndRestoreSilently() {
        queryProductDetailsInternal()
        queryOwnedPurchasesInternal(userInitiated = false)
    }

    private suspend fun queryProductDetailsInternal(): Map<String, ProductDetails> {
        val productList = SkuCatalog.ALL_IN_APP_SKUS.map { sku ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(sku)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        val result = withContext(Dispatchers.IO) {
            billingClient.queryProductDetails(params)
        }
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.w(TAG, "queryProductDetails failed (${result.billingResult.responseCode}): ${result.billingResult.debugMessage}")
            return cachedProductDetails
        }

        val detailsMap = result.productDetailsList.orEmpty().associateBy { it.productId }
        cachedProductDetails = detailsMap

        val proPrice = detailsMap[SkuCatalog.SKU_PRO_LIFETIME]
            ?.oneTimePurchaseOfferDetailsList
            ?.firstOrNull()
            ?.formattedPrice
        val seriesPrice = detailsMap[SkuCatalog.SKU_SERIES_VOLUME1]
            ?.oneTimePurchaseOfferDetailsList
            ?.firstOrNull()
            ?.formattedPrice

        _productPrices.value = StoreProductPrices(
            proFormattedPrice = proPrice,
            seriesFormattedPrice = seriesPrice
        )
        return detailsMap
    }

    override fun queryOwnedPurchases(userInitiated: Boolean) {
        scope.launch {
            if (!billingClient.isReady) {
                connect()
                if (userInitiated) {
                    _events.tryEmit(BillingEvent.Error("Play Billing not ready yet"))
                }
                return@launch
            }
            queryOwnedPurchasesInternal(userInitiated = userInitiated)
        }
    }

    private suspend fun queryOwnedPurchasesInternal(userInitiated: Boolean) {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val purchasesResult = withContext(Dispatchers.IO) {
            billingClient.queryPurchasesAsync(params)
        }
        if (purchasesResult.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.w(
                TAG,
                "queryPurchasesAsync failed (${purchasesResult.billingResult.responseCode}): ${purchasesResult.billingResult.debugMessage}"
            )
            if (userInitiated) {
                _events.tryEmit(BillingEvent.Error(purchasesResult.billingResult.debugMessage.ifBlank { null }))
            }
            return
        }
        processCompletedPurchases(
            purchases = purchasesResult.purchasesList,
            fromRestore = true,
            userInitiatedRestore = userInitiated
        )
    }

    override fun launchPurchase(activity: Activity?, sku: String) {
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            _events.tryEmit(BillingEvent.Error("No active screen for Play Billing flow"))
            return
        }
        scope.launch {
            if (!billingClient.isReady) {
                connect()
                _events.tryEmit(BillingEvent.Error("Google Play Store connection unavailable"))
                return@launch
            }
            val details = cachedProductDetails[sku] ?: queryProductDetailsInternal()[sku]
            if (details == null) {
                _events.tryEmit(BillingEvent.Error("SKU $sku not configured in Play Console"))
                return@launch
            }

            val offerToken = details.oneTimePurchaseOfferDetailsList
                ?.firstOrNull()
                ?.offerToken
            val productParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
            if (!offerToken.isNullOrBlank()) {
                productParamsBuilder.setOfferToken(offerToken)
            }

            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productParamsBuilder.build()))
                .build()

            val launchResult = billingClient.launchBillingFlow(activity, flowParams)
            if (launchResult.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
                queryOwnedPurchasesInternal(userInitiated = true)
            } else if (launchResult.responseCode != BillingClient.BillingResponseCode.OK &&
                launchResult.responseCode != BillingClient.BillingResponseCode.USER_CANCELED
            ) {
                Log.w(TAG, "launchBillingFlow failed (${launchResult.responseCode}): ${launchResult.debugMessage}")
                _events.tryEmit(BillingEvent.Error(launchResult.debugMessage.ifBlank { null }))
            }
        }
    }

    private suspend fun processCompletedPurchases(
        purchases: List<Purchase>,
        fromRestore: Boolean,
        userInitiatedRestore: Boolean
    ) {
        var sawPending = false
        val grantedSkus = mutableSetOf<String>()

        for (purchase in purchases) {
            when (purchase.purchaseState) {
                Purchase.PurchaseState.PURCHASED -> {
                    val ackOk = if (purchase.isAcknowledged) {
                        true
                    } else {
                        val ackParams = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()
                        val ackResult = withContext(Dispatchers.IO) {
                            billingClient.acknowledgePurchase(ackParams)
                        }
                        if (ackResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            true
                        } else {
                            Log.w(TAG, "acknowledgePurchase failed (${ackResult.responseCode}): ${ackResult.debugMessage}")
                            false
                        }
                    }

                    if (ackOk) {
                        for (sku in purchase.products) {
                            if (sku in SkuCatalog.ALL_IN_APP_SKUS) {
                                grantedSkus.add(sku)
                            }
                        }
                    } else if (!fromRestore) {
                        _events.tryEmit(BillingEvent.Error("Purchase acknowledgment pending retry"))
                    }
                }
                Purchase.PurchaseState.PENDING -> {
                    sawPending = true
                }
                else -> {
                    Log.i(TAG, "Ignoring purchase with unspecified state")
                }
            }
        }

        if (fromRestore) {
            if (grantedSkus.isNotEmpty() || userInitiatedRestore) {
                _events.tryEmit(BillingEvent.RestoredPurchases(grantedSkus, userInitiatedRestore))
            }
        } else {
            for (sku in grantedSkus) {
                _events.tryEmit(BillingEvent.PurchaseGranted(sku))
            }
        }

        if (sawPending && !fromRestore) {
            _events.tryEmit(BillingEvent.PurchasePending)
        }
    }

    override fun destroy() {
        _isReady.value = false
        try {
            billingClient.endConnection()
        } catch (e: Exception) {
            Log.w(TAG, "Ignoring exception while ending BillingClient", e)
        }
        scope.cancel()
    }

    companion object {
        private const val TAG = "PlayBillingManager"
    }
}
