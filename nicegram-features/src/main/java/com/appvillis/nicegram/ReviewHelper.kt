package com.appvillis.nicegram

import android.app.Activity
import android.util.Log
import com.appvillis.nicegram.domain.NicegramSessionCounter
import com.google.android.play.core.review.ReviewManagerFactory

object ReviewHelper {
    private const val SHOW_REVIEW_ON_SESSION = 3
    var nicegramSessionCounter: NicegramSessionCounter? = null
    var wasShownThisSession = false

    fun launchReview(activity: Activity) {
        if ((nicegramSessionCounter?.sessionsCount ?: 0) != SHOW_REVIEW_ON_SESSION && !wasShownThisSession) {
            return
        }
        wasShownThisSession = true

        val manager = ReviewManagerFactory.create(activity)
        val request = manager.requestReviewFlow()

        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("ReviewHelper","review show")

                val reviewInfo = task.result
                manager.launchReviewFlow(activity, reviewInfo)
            }
        }
    }
}