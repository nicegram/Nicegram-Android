package com.appvillis.nicegram

import android.app.Activity
import android.content.Context
import com.google.android.play.core.review.ReviewManagerFactory
import dagger.hilt.EntryPoints
import timber.log.Timber

object ReviewHelper {
    private fun entryPoint(context: Context) = EntryPoints.get(context.applicationContext, NicegramAssistantEntryPoint::class.java)

    private const val SHOW_REVIEW_ON_SESSION = 3

    private var wasShownThisSession = false

    fun launchReview(activity: Activity) {
        if (entryPoint(activity).nicegramSessionCounter().sessionsCount != SHOW_REVIEW_ON_SESSION && !wasShownThisSession) {
            return
        }
        wasShownThisSession = true

        val manager = ReviewManagerFactory.create(activity)
        val request = manager.requestReviewFlow()

        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Timber.d("ReviewHelper review show")

                val reviewInfo = task.result
                manager.launchReviewFlow(activity, reviewInfo)
            }
        }
    }
}