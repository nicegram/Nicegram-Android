package com.appvillis.nicegram.domain

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.SkuDetails
import kotlinx.coroutines.flow.Flow

interface BillingManager {
    fun initializeBilling()
    fun restore()

    val billingClient: BillingClient

    val sub: Sub?

    val userHasActiveSub: Boolean

    val subPurchasedFlow: Flow<Unit>

    class Sub(val price: String, val sku: SkuDetails)
}