package com.appvillis.nicegram

import android.app.Activity
import com.appvillis.assistant_core.InChatMainActivity
import com.appvillis.assistant_core.MainActivity
import com.appvillis.core_resources.domain.TgResourceProvider
import com.appvillis.feature_ai_chat.domain.ClearDataUseCase
import com.appvillis.feature_ai_chat.domain.UseResultManager
import com.appvillis.feature_ai_chat.domain.entity.AiCommand
import com.appvillis.feature_ai_chat.domain.usecases.GetBalanceTopUpRequestUseCase
import com.appvillis.feature_ai_chat.domain.usecases.GetChatCommandsUseCase
import com.appvillis.feature_auth.AuthNavHelper
import com.appvillis.feature_nicegram_billing.domain.RequestInAppsUseCase
import com.appvillis.nicegram.NicegramScopes.uiScope
import com.appvillis.nicegram_wallet.wallet_contacts.domain.WalletContact
import com.appvillis.rep_user.domain.GetUserStatusUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

object AiChatBotHelper {
    var getChatCommandsUseCase: GetChatCommandsUseCase? = null
    var getUserStatusUseCase: GetUserStatusUseCase? = null
    var requestInAppsUseCase: RequestInAppsUseCase? = null
    var getBalanceTopUpRequestUseCase: GetBalanceTopUpRequestUseCase? = null
    var useResultManager: UseResultManager? = null
    var clearDataUseCase: ClearDataUseCase? = null
    var tgResourceProvider: TgResourceProvider? = null

    fun setUseResultListener(listener: UseResultManager.UseResultLister) {
        useResultManager?.listener = listener
    }

    fun launchAiBot(activity: Activity, telegramId: Long, dialog: Boolean) {
        val getUserStatusUseCase = getUserStatusUseCase ?: return
        if (getUserStatusUseCase.isUserLoggedIn) {
            if (dialog) MainActivity.launchAiBotDialog(null, null, activity, telegramId)
            else MainActivity.launchAiBot(activity, telegramId)
        } else {
            if (dialog) AuthNavHelper.authBack = true
            MainActivity.launchAiGreetings(activity, telegramId)
        }
    }

    fun launchInChatWidget(activity: Activity, id: String, firstName: String?, lastName: String?, username: String?, img: String, telegramId: Long) {
        InChatMainActivity.launch(activity, WalletContact(
            id,
            nameString(firstName, lastName),
            usernameString(username),
            img
        ), telegramId)
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

    private var topUpRequestJob: Job? = null

    fun registerTopUpCallback(callbackActivity: Activity, telegramId: Long) {
        val getBalanceTopUpRequestUseCase = getBalanceTopUpRequestUseCase ?: return
        topUpRequestJob = uiScope.launch {
            getBalanceTopUpRequestUseCase().collect {
                MainActivity.launchAiTopUp(callbackActivity, false, telegramId)
            }
        }
    }

    fun unregisterTopUpCallback() {
        topUpRequestJob?.cancel()
    }

    fun getContextCommands(): List<AiCommand> {
        val getChatCommandsUseCase = getChatCommandsUseCase ?: return listOf()
        return getChatCommandsUseCase.contextCommands
    }

    fun onContextCommandClick(activity: Activity, command: AiCommand, text: String, telegramId: Long) {
        val getUserStatusUseCase = getUserStatusUseCase ?: return
        if (getUserStatusUseCase.isUserLoggedIn) {
            MainActivity.launchAiBotDialog(command, text, activity, telegramId)
        } else {
            AuthNavHelper.authBack = true
            MainActivity.launchAiGreetings(activity, telegramId)
        }
    }
}