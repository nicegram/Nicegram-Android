package com.appvillis.nicegram.data

import android.content.SharedPreferences
import com.appvillis.nicegram.domain.NicegramFeaturesPrefsRepository

class NicegramFeaturesPrefsRepositoryImpl(private val prefs: SharedPreferences) : NicegramFeaturesPrefsRepository {
    companion object {
        private const val PREF_HAS_SEEN_ONBOARDING = "PREF_HAS_SEEN_ONBOARDING"
    }

    override var hasSeenOnboarding: Boolean
        get() = prefs.getBoolean(PREF_HAS_SEEN_ONBOARDING, false)
        set(value) {
            prefs.edit().putBoolean(PREF_HAS_SEEN_ONBOARDING, value).apply()
        }
}