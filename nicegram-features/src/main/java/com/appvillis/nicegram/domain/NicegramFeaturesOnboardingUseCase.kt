package com.appvillis.nicegram.domain

class NicegramFeaturesOnboardingUseCase(private val prefsRepository: NicegramFeaturesPrefsRepository) {
    var hasSeenOnboarding get() = prefsRepository.hasSeenOnboarding
        set(value) {
            prefsRepository.hasSeenOnboarding = value
        }
}