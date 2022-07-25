package com.appvillis.nicegram

import com.appvillis.feature_nicegram_assistant.domain.GetNicegramOnboardingStatusUseCase
import com.appvillis.feature_nicegram_assistant.domain.GetSpecialOfferUseCase
import com.appvillis.feature_nicegram_assistant.domain.SetNicegramOnboardingStatusUseCase
import com.appvillis.feature_nicegram_assistant.domain.SpecialOffersRepository
import com.appvillis.rep_user.domain.AppSessionControlUseCase

object NicegramAssistantHelper {
    var getNicegramOnboardingStatusUseCase: GetNicegramOnboardingStatusUseCase? = null
    var setNicegramOnboardingStatusUseCase: SetNicegramOnboardingStatusUseCase? = null
    var getSpecialOfferUseCase: GetSpecialOfferUseCase? = null
    var appSessionControlUseCase: AppSessionControlUseCase? = null

    fun setOnboardingShown() {
        setNicegramOnboardingStatusUseCase?.setOnBoardingWasShown(true)
    }

    fun wasOnboardingShown(): Boolean {
        return getNicegramOnboardingStatusUseCase?.onboardingWasShown ?: true
    }

    fun getSpecialOffer(): SpecialOffersRepository.SpecialOffer? {
        if (!getSpecialOfferUseCase!!.haveSeenCurrentOffer() && getSpecialOfferUseCase!!.canShowSpecialOfferCurrentSession(
                appSessionControlUseCase!!.appSessionNumber
            )
        ) {
            return getSpecialOfferUseCase!!.specialOffer
        }

        return null
    }
}