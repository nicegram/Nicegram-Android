package app.nicegram

import android.view.MotionEvent
import android.view.ViewConfiguration

object NicegramSecondTapHelper {

    private const val TAP_TIMEOUT = 1500

    private var lastTapTimestamp: Long = 0

    fun shouldIgnoreSecondTap(): Boolean {
        val time = System.currentTimeMillis()
        if (time - lastTapTimestamp < TAP_TIMEOUT) {
            return true
        }
        lastTapTimestamp = time
        return false
    }

    fun isLongClick(motionEvent: MotionEvent): Boolean {
        return motionEvent.eventTime - motionEvent.downTime >= ViewConfiguration.getLongPressTimeout()
    }
}