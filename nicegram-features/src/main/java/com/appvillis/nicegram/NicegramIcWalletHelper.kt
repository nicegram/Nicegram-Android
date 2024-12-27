package com.appvillis.nicegram

import android.app.Activity
import com.appvillis.assistant_core.InChatMainActivity
import com.appvillis.nicegram_wallet.module_bridge.InChatResultManager
import com.appvillis.nicegram_wallet.wallet_contacts.domain.WalletContact

object NicegramIcWalletHelper {
    var inChatResultManager: InChatResultManager? = null

    fun setUseResultListener(listener: InChatResultManager.InChatResultLister) {
        inChatResultManager?.listener = listener
    }

    fun launchInChatWidget(
        activity: Activity,
        id: String,
        firstName: String?,
        lastName: String?,
        username: String?,
        img: String
    ) {
        InChatMainActivity.launch(
            activity, WalletContact(
                id,
                nameString(firstName, lastName),
                usernameString(username),
                img
            )
        )
    }

    private fun nameString(firstName: String?, lastName: String?): String {
        return if (firstName.isNullOrEmpty() && lastName.isNullOrEmpty()) ""
        else if (firstName.isNullOrEmpty() && !lastName.isNullOrEmpty()) lastName
        else if (lastName.isNullOrEmpty() && !firstName.isNullOrEmpty()) firstName
        else "$firstName $lastName"
    }

    private fun usernameString(username: String?): String {
        return if (username.isNullOrEmpty()) ""
        else "@${username}"
    }

}
