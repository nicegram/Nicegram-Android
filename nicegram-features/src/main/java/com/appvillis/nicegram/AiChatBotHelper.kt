package com.appvillis.nicegram

import android.app.Activity
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

    fun launchAiBot(activity: Activity, dialog: Boolean) {
        val getUserStatusUseCase = getUserStatusUseCase ?: return
        if (getUserStatusUseCase.isUserLoggedIn) {
            //if (dialog) MainActivity.launchAiBotDialog(null, null, activity, telegramId)
            if (dialog) MainActivity.launchAiBot(activity)
            else MainActivity.launchAiBot(activity)
        } else {
            if (dialog) AuthNavHelper.authBack = true
            MainActivity.launchAiGreetings(activity)
        }
    }

    private var topUpRequestJob: Job? = null

    fun registerTopUpCallback(callbackActivity: Activity) {
        val getBalanceTopUpRequestUseCase = getBalanceTopUpRequestUseCase ?: return
        topUpRequestJob = uiScope.launch {
            getBalanceTopUpRequestUseCase().collect {
                MainActivity.launchAiTopUp(callbackActivity, false)
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

    fun onContextCommandClick(activity: Activity, command: AiCommand, text: String) {
        val getUserStatusUseCase = getUserStatusUseCase ?: return
        if (getUserStatusUseCase.isUserLoggedIn) {
            MainActivity.launchAiBotDialog(command, text, activity)
        } else {
            AuthNavHelper.authBack = true
            MainActivity.launchAiGreetings(activity)
        }
    }
}
