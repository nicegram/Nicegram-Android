package com.appvillis.nicegram.domain

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.SkuDetails
import com.appvillis.feature_powerball.domain.SocialInfoProvider
import kotlinx.coroutines.flow.Flow

interface BillingManager : SocialInfoProvider {
    fun initializeBilling()
    fun restore()

    val billingClient: BillingClient

    val sub: Sub?

    val userHasActiveSub: Boolean

    val subPurchasedFlow: Flow<Unit>

    class Sub(val price: String, val sku: SkuDetails)
}