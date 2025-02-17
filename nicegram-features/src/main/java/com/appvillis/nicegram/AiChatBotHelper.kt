package com.appvillis.nicegram

import android.app.Activity
import android.content.Context
import com.appvillis.assistant_core.MainActivity
import com.appvillis.feature_ai_chat.domain.UseResultManager
import com.appvillis.feature_ai_chat.domain.entity.AiCommand
import com.appvillis.feature_auth.AuthNavHelper
import com.appvillis.nicegram.NicegramScopes.uiScope
import dagger.hilt.EntryPoints
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

object AiChatBotHelper {
    private fun entryPoint(context: Context) =
        EntryPoints.get(context.applicationContext, NicegramAssistantEntryPoint::class.java)

    fun getClearDataUseCase(context: Context) = entryPoint(context).clearDataUseCase()

    fun setUseResultListener(context: Context, listener: UseResultManager.UseResultLister) {
        entryPoint(context).useResultManager().listener = listener
    }

    fun launchAiBot(activity: Activity, dialog: Boolean) {
        val getUserStatusUseCase = entryPoint(activity).getUserStatusUseCase()
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
        val getBalanceTopUpRequestUseCase = entryPoint(callbackActivity).getBalanceTopUpRequestUseCase()
        topUpRequestJob = uiScope.launch {
            getBalanceTopUpRequestUseCase().collect {
                MainActivity.launchAiTopUp(callbackActivity, false)
            }
        }
    }

    fun unregisterTopUpCallback() {
        topUpRequestJob?.cancel()
    }

    fun getContextCommands(context: Context): List<AiCommand> {
        return entryPoint(context).getChatCommandsUseCase().contextCommands
    }

    fun onContextCommandClick(activity: Activity, command: AiCommand, text: String) {
        val getUserStatusUseCase = entryPoint(activity).getUserStatusUseCase()
        if (getUserStatusUseCase.isUserLoggedIn) {
            MainActivity.launchAiBotDialog(command, text, activity)
        } else {
            AuthNavHelper.authBack = true
            MainActivity.launchAiGreetings(activity)
        }
    }
}
