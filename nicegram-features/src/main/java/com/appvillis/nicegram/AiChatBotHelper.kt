package com.appvillis.nicegram

import android.app.Activity
import android.content.Context
import com.appvillis.assistant_core.MainActivity
import com.appvillis.feature_ai_chat.domain.UseResultManager
import com.appvillis.feature_auth.AuthNavHelper
import dagger.hilt.EntryPoints

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
            if (dialog) MainActivity.launchAiBot(activity)
            else MainActivity.launchAiBot(activity)
        } else {
            if (dialog) AuthNavHelper.authBack = true
            MainActivity.launchAiGreetings(activity)
        }
    }
}
