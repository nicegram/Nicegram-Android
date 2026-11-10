package com.appvillis.nicegram

import android.content.Context
import dagger.hilt.EntryPoints

object AiChatBotHelper {
    private fun entryPoint(context: Context) =
        EntryPoints.get(context.applicationContext, NicegramAssistantEntryPoint::class.java)

    fun getClearDataUseCase(context: Context) = entryPoint(context).clearDataUseCase()
}
