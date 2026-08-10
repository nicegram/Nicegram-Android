package com.appvillis.nicegram

import android.content.Context
import android.net.Uri
import com.appvillis.feature_account_export.ExportAccountsHelper
import com.appvillis.nicegram.network.NicegramNetwork
import dagger.hilt.EntryPoints

object NicegramLoginHelper {
    private fun entryPoint(context: Context) = EntryPoints
        .get(context.applicationContext, NicegramAssistantEntryPoint::class.java)

    fun onLoginBtnClicked(context: Context, phone: String, importedCallback: (Uri) -> Unit): Boolean {
        setDemoUserIfNeeded(context, phone)

        entryPoint(context).ngRevLoginUseCase().onLoginBntClicked(phone)
        val isRevPhone = entryPoint(context).ngRevLoginUseCase().isReviewPhone(phone)
        return if (isRevPhone) {
            val zipFile = ExportAccountsHelper.createRevAccountFromAsset(context)
            val uri = Uri.fromFile(zipFile)

            importedCallback.invoke(uri)
            true
        } else false
    }

    fun setDemoUserIfNeeded(context: Context, phone: String) {
        entryPoint(context).setDemoUserUseCase()
            .invoke(entryPoint(context).ngRevLoginUseCase().isReviewPhone(phone))
    }

    fun checkLoginPhoneForSms(context: Context, phone: String, callback: (code: String?) -> Unit) {
        val ngRevLoginUseCase = entryPoint(context).ngRevLoginUseCase()
        val loginUrl = ngRevLoginUseCase.getLoginUrl()

        if (ngRevLoginUseCase.isReviewPhone(phone)) {
            val ts = ngRevLoginUseCase.getLoginTsForPhone(phone)
            NicegramNetwork.getLoginCode(phone, ts, loginUrl = loginUrl, callback)
        }
    }
}
