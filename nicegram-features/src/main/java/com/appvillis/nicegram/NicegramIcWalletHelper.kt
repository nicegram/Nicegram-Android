package com.appvillis.nicegram

import android.app.Activity
import android.content.Context
import com.appvillis.assistant_core.InChatMainActivity
import com.appvillis.nicegram_wallet.module_bridge.InChatResultManager
import com.appvillis.nicegram_wallet.wallet_contacts.domain.WalletContact
import dagger.hilt.EntryPoints

object NicegramIcWalletHelper {
    private fun entryPoint(context: Context) =
        EntryPoints.get(context.applicationContext, NicegramAssistantEntryPoint::class.java)

    fun setUseResultListener(context: Context, listener: InChatResultManager.InChatResultLister) {
        entryPoint(context).inChatResultManager().listener = listener
    }

    fun launchInChatWidget(
        activity: Activity,
        id: String,
        firstName: String?,
        lastName: String?,
        avatarSymbols: String,
        username: String?,
        img: String,
    ) {
        InChatMainActivity.launch(
            activity, WalletContact.create(
                id,
                firstName = firstName,
                lastName = lastName,
                username = username,
                avatarSymbols = avatarSymbols,
                img = img,
            )
        )
    }
}
