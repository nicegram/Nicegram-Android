package com.appvillis.nicegram.presentation

import androidx.lifecycle.ViewModel
import com.appvillis.nicegram.domain.NicegramFeaturesOnboardingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NicegramOnboardingViewModel @Inject constructor(
    private val nicegramFeaturesOnboardingUseCase: NicegramFeaturesOnboardingUseCase,
) : ViewModel() {

    fun onViewCreated() {
        nicegramFeaturesOnboardingUseCase.hasSeenOnboarding = true
    }
}
