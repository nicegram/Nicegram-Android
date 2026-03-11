package com.appvillis.nicegram

import android.content.Context
import com.appvillis.feature_analytics.data.AnalyticsEvent
import com.appvillis.feature_analytics.data.AnalyticsTrackEvent
import com.appvillis.feature_analytics.data.AnalyticsValue
import com.appvillis.feature_analytics.domain.AnalyticsEntryPoint
import dagger.hilt.EntryPoints

object AnalyticsHelper {

    private fun entryPoint(context: Context) = EntryPoints
        .get(context.applicationContext, AnalyticsEntryPoint::class.java)

    fun logEvent(context: Context, name: String, params: Map<String, AnalyticsValue>?) {
        entryPoint(context).analyticsManager().logEvent(name, params ?: mapOf())
    }

    fun logEvent(context: Context, event: AnalyticsTrackEvent) {
        entryPoint(context).analyticsManager().logEvent(event)
    }

    fun logOneTimePerSessionEvent(context: Context, name: String) {
        entryPoint(context).analyticsManager().logEventOncePerSession(name)
    }
}
