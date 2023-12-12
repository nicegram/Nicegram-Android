package com.appvillis.nicegram

import com.appvillis.feature_analytics.domain.AnalyticsManager

object AnalyticsHelper {
    var analyticsManager: AnalyticsManager? = null

    val eventsLoggedThisSession = mutableListOf<String>()

    fun logEvent(name: String, params: Map<String, String>?) {
        analyticsManager?.logEvent(name, params ?: mapOf())
    }

    fun logOneTimePerSessionEvent(name: String, params: Map<String, String>?) {
        if (eventsLoggedThisSession.contains(name)) return
        eventsLoggedThisSession.add(name)

        logEvent(name, params)
    }
}