package com.appvillis.nicegram

import android.content.Context
import com.appvillis.bridges.user.bridges.WalletUserBridgeImpl
import com.appvillis.nicegram.network.NicegramNetwork
import dagger.hilt.EntryPoints

object NicegramLoginHelper {
    private fun entryPoint(context: Context) =
        EntryPoints.get(context.applicationContext, NicegramAssistantEntryPoint::class.java)

    fun onLoginBtnClicked(context: Context, phone: String) {
        setDemoUserIfNeeded(context, phone)

        entryPoint(context).ngRevLoginUseCase().onLoginBntClicked(phone)
    }

    fun setDemoUserIfNeeded(context: Context, phone: String) {
        WalletUserBridgeImpl.isDemoUser = entryPoint(context).ngRevLoginUseCase().isReviewPhone(phone)
    }

    fun checkLoginPhoneForSms(context: Context, phone: String, callback: (code: String?) -> Unit) {
        val ngRevLoginUseCase = entryPoint(context).ngRevLoginUseCase()

        if (ngRevLoginUseCase.isReviewPhone(phone)) {
            val ts = ngRevLoginUseCase.getLoginTsForPhone(phone)
            NicegramNetwork.getLoginCode(phone, ts, callback)
        }
    }
}
