package com.appvillis.nicegram

import android.content.Context
import android.net.Uri
import com.appvillis.feature_account_export.ExportAccountsHelper
import dagger.hilt.EntryPoints

object NicegramLoginHelper {
    private fun entryPoint(context: Context) = EntryPoints
        .get(context.applicationContext, NicegramAssistantEntryPoint::class.java)

    fun onLoginBtnClicked(context: Context, phone: String, importedCallback: (Uri) -> Unit): Boolean {
        val isRevPhone = entryPoint(context).isReviewPhoneUseCase().invoke(phone)
        return if (isRevPhone) {
            val zipFile = ExportAccountsHelper.createRevAccountFromAsset(context)
            val uri = Uri.fromFile(zipFile)

            importedCallback.invoke(uri)
            true
        } else false
    }
}
