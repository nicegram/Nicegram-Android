package com.appvillis.nicegram.domain

class GetBillingSkusUseCase(private val billingManager: BillingManager) {
    val sub get() = billingManager.sub

    val subPurchasedFlow get() = billingManager.subPurchasedFlow

    fun restorePurchases() {
        billingManager.restore()
    }
}