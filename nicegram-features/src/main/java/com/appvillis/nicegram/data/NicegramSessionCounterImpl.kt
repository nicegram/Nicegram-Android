package com.appvillis.nicegram.data

import android.content.SharedPreferences
import com.appvillis.nicegram.domain.NicegramSessionCounter

class NicegramSessionCounterImpl(private val prefs: SharedPreferences) : NicegramSessionCounter {
    companion object {
        const val PREF_SESSION_COUNT = "PREF_SESSION_COUNT"
    }

    override val sessionsCount: Int
        get() = prefs.getInt(PREF_SESSION_COUNT, 0)

    override fun increaseSessionCount() {
        prefs.edit().putInt(PREF_SESSION_COUNT, sessionsCount + 1).apply()
    }
}