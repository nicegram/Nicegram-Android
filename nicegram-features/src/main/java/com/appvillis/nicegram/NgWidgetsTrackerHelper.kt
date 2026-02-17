package com.appvillis.nicegram

import android.content.Context
import com.appvillis.feature_chat_widgets.NgWidgetsEntryPoint
import dagger.hilt.EntryPoints
import kotlinx.coroutines.launch
import timber.log.Timber

object NgWidgetsTrackerHelper {

    private fun entryPoint(context: Context) = EntryPoints
        .get(context.applicationContext, NgWidgetsEntryPoint::class.java)

    fun viewTrackVisibilityHelper(isVisible: Boolean, context: Context) {
        val entryPoints = entryPoint(context)

        entryPoints.appScope().launch {
            entryPoints.pinnedBannerManager().apply {
                if (isVisible) {
                    Timber.d("onBannerVisible")
                    onBannerVisible()
                } else {
                    onExitFromScreen()
                    Timber.d("onExitFromScreen")
                }
            }
        }
    }
}
