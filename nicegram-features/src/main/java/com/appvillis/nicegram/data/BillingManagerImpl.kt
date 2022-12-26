package com.appvillis.nicegram.data

import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.appvillis.feature_powerball.domain.SocialInfoProvider
import com.appvillis.nicegram.domain.BillingManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber

class BillingManagerImpl(
    context: Context,
    private val appCoroutineScope: CoroutineScope
) :
    BillingManager,
    BillingClientStateListener, PurchasesUpdatedListener,
    PurchasesResponseListener {
    companion object {
        private const val TAG = "BillingManagerImpl"

        private const val BILLING_RECONNECT_DELAY = 1000L

        private const val SUB_ID = "nicegram_premium"
    }

    private val _subPurchasedFlow = MutableSharedFlow<Unit>()

    override val subPurchasedFlow: Flow<Unit>
        get() = _subPurchasedFlow

    private var _userHasActiveSub = false
        set(value) {
            field = value

            if (value) {
                appCoroutineScope.launch {
                    _subPurchasedFlow.emit(Unit)
                }
            }
        }
    override val userHasActiveSub: Boolean
        get() = _userHasActiveSub

    private var _sub: BillingManager.Sub? = null
    override val sub: BillingManager.Sub?
        get() = _sub

    override var billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    override fun initializeBilling() {
        connectToBilling()
    }

    override fun restore() {
        if (billingClient.isReady) {
            billingClient.queryPurchasesAsync(
                BillingClient.SkuType.SUBS,
                this@BillingManagerImpl
            )
        } else {
            connectToBilling()
        }
    }

    private fun connectToBilling() {
        billingClient.startConnection(this)
    }

    private suspend fun querySubsSkuDetails() {
        val skuList = ArrayList<String>()
        skuList.add(SUB_ID)
        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList).setType(BillingClient.SkuType.SUBS)

        val skuDetailsResult = withContext(Dispatchers.IO) {
            billingClient.querySkuDetails(params.build())
        }

        val subSku = skuDetailsResult.skuDetailsList?.find { it.sku == SUB_ID } ?: return

        this._sub = BillingManager.Sub(subSku.price, subSku)
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            appCoroutineScope.launch {
                querySubsSkuDetails()

                billingClient.queryPurchasesAsync(
                    BillingClient.SkuType.SUBS,
                    this@BillingManagerImpl
                )
            }
        }
    }

    override fun onBillingServiceDisconnected() {
        appCoroutineScope.launch {
            delay(BILLING_RECONNECT_DELAY)
            connectToBilling()
        }
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        Timber.d(TAG, "onPurchasesUpdated ${billingResult.responseCode} ${billingResult.debugMessage}")

        purchases?.forEach {
            Timber.d(
                TAG,
                "Purchase, state: ${it.purchaseState} token: ${it.purchaseToken} skus: ${
                    it.skus.joinToString(
                        ","
                    )
                }"
            )
            if (it.purchaseState == Purchase.PurchaseState.PURCHASED) {
                if (!it.isAcknowledged) acknowledge(it)
                _userHasActiveSub = true
                appCoroutineScope.launch {
                    _premiumInfo.emit(SocialInfoProvider.PremiumInfo(it.purchaseToken, it.skus[0]))
                }
            }
        }
    }

    private fun acknowledge(purchase: Purchase) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(
            acknowledgePurchaseParams
        ) { billingResult ->
            Timber.d(
                TAG,
                "Acknowledge ended. ${billingResult.responseCode} ${billingResult.debugMessage}"
            )
        }
    }

    override fun onQueryPurchasesResponse(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>
    ) {
        _userHasActiveSub = false

        purchases.forEach {
            Log.d(
                TAG,
                "onQueryPurchasesResponse ${it.purchaseToken} ${it.purchaseState} ${it.purchaseTime}"
            )

            if (it.purchaseState == Purchase.PurchaseState.PURCHASED) {
                if (!it.isAcknowledged) acknowledge(it)
                _userHasActiveSub = true
                appCoroutineScope.launch {
                    _premiumInfo.emit(SocialInfoProvider.PremiumInfo(it.purchaseToken, it.skus[0]))
                }
            }
        }

        if (!_userHasActiveSub) {
            appCoroutineScope.launch {
                _premiumInfo.emit(null)
            }
        }
    }

    private val _premiumInfo = MutableStateFlow<SocialInfoProvider.PremiumInfo?>(null)
    override val premiumInfo: Flow<SocialInfoProvider.PremiumInfo?>
        get() = _premiumInfo
}