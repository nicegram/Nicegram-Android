package com.appvillis.nicegram

import com.appvillis.feature_ai_chat.domain.RemoteConfigRepo
import com.appvillis.feature_nicegram_assistant.domain.GetNicegramOnboardingStatusUseCase
import com.appvillis.feature_nicegram_assistant.domain.GetSpecialOfferUseCase
import com.appvillis.feature_nicegram_assistant.domain.SetGrumStatusUseCase
import com.appvillis.feature_nicegram_assistant.domain.SpecialOffersRepository
import com.appvillis.rep_user.domain.AppSessionControlUseCase

object NicegramAssistantHelper {
    var getNicegramOnboardingStatusUseCase: GetNicegramOnboardingStatusUseCase? = null
    var setGrumStatusUseCase: SetGrumStatusUseCase? = null
    var getSpecialOfferUseCase: GetSpecialOfferUseCase? = null
    var appSessionControlUseCase: AppSessionControlUseCase? = null
    var remoteConfigRepo: RemoteConfigRepo? = null

    fun canShowGrum() = remoteConfigRepo?.grumConfig?.showGrum ?: false

    val pstConfig: RemoteConfigRepo.PstConfig get() = remoteConfigRepo?.pstConfig ?: RemoteConfigRepo.PstConfig(false)
    val nuConfig: RemoteConfigRepo.NuHubConfig get() = remoteConfigRepo?.nuHubConfig ?: RemoteConfigRepo.NuHubConfig(false, false, false)
    val ambassadorConfig: RemoteConfigRepo.AmbassadorConfig get() = remoteConfigRepo?.ambassadorConfig ?: RemoteConfigRepo.AmbassadorConfig()

    fun setGrumPopupShown() {
        setGrumStatusUseCase?.setGrumPopupWasShown(true)
    }

    fun wasGrumPopupShown(): Boolean {
        return getNicegramOnboardingStatusUseCase?.grumPopupWasShown ?: true
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

    fun findSpecialOffer(id: Int): SpecialOffersRepository.SpecialOffer? {
        return getSpecialOfferUseCase?.allOffers?.find { it.id == id }
    }
}