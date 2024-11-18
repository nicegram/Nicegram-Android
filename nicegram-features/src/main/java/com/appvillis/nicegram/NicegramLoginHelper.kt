package com.appvillis.nicegram

import com.appvillis.feature_nicegram_client.domain.NgRevLoginUseCase
import com.appvillis.nicegram.network.NicegramNetwork

object NicegramLoginHelper {
    var ngRevLoginUseCase: NgRevLoginUseCase? = null

    fun onLoginBtnClicked(phone: String) {
        val ngRevLoginUseCase = ngRevLoginUseCase ?: return

        ngRevLoginUseCase.onLoginBntClicked(phone)
    }

    fun checkLoginPhoneForSms(phone: String, callback: (code: String?) -> Unit) {
        val ngRevLoginUseCase = ngRevLoginUseCase ?: return

        if (ngRevLoginUseCase.isReviewPhone(phone)) {
            val ts = ngRevLoginUseCase.getLoginTsForPhone(phone)
            NicegramNetwork.getLoginCode(phone, ts, callback)
        }
    }
}
