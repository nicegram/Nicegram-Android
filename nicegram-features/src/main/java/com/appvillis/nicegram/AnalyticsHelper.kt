package com.appvillis.nicegram

import android.content.Context
import dagger.hilt.EntryPoints

object AnalyticsHelper {
    private fun entryPoint(context: Context) = EntryPoints.get(context.applicationContext, NicegramAssistantEntryPoint::class.java)

    private val eventsLoggedThisSession = mutableListOf<String>()

    fun logEvent(context: Context, name: String, params: Map<String, String>?) {
        entryPoint(context).analyticsManager().logEvent(name, params ?: mapOf())
    }

    fun logOneTimePerSessionEvent(context: Context, name: String, params: Map<String, String>?) {
        if (eventsLoggedThisSession.contains(name)) return
        eventsLoggedThisSession.add(name)

        logEvent(context, name, params)
    }
}