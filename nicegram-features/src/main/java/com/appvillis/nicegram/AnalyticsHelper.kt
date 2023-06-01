package com.appvillis.nicegram

import com.appvillis.feature_analytics.domain.AnalyticsManager

object AnalyticsHelper {
    var analyticsManager: AnalyticsManager? = null

    fun logEvent(name: String, params: Map<String, String>?) {
        analyticsManager?.logEvent(name, params ?: mapOf())
    }
}