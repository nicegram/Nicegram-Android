package com.appvillis.nicegram

import com.appvillis.nicegram_wallet.wallet_dapps.domain.BrowserResponseManager
import com.appvillis.nicegram_wallet.wallet_dapps.domain.TgBrowserBridgeFactory
import javax.inject.Inject

class TgBridgeDependenciesHolder @Inject constructor(val tgBrowserBridgeFactory: TgBrowserBridgeFactory, val browserResponseManager: BrowserResponseManager) {
    companion object {
        var instance: TgBridgeDependenciesHolder? = null

        fun getTgBrowserBridgeFactory() = instance?.tgBrowserBridgeFactory
        fun getBrowserResponseManager() = instance?.browserResponseManager
    }
}