package com.appvillis.nicegram

import com.appvillis.feature_ai_chat.domain.AiChatRemoteConfigRepo
import com.appvillis.feature_nicegram_assistant.domain.GetNicegramOnboardingStatusUseCase
import com.appvillis.feature_nicegram_assistant.domain.GetSetPstStartedStatusUseCase
import com.appvillis.feature_nicegram_assistant.domain.GetSpecialOfferUseCase
import com.appvillis.feature_nicegram_assistant.domain.SpecialOffersRepository
import com.appvillis.rep_placements.domain.GetChatPlacementsUseCase
import com.appvillis.rep_user.domain.AppSessionControlUseCase

object NicegramAssistantHelper {
    var getNicegramOnboardingStatusUseCase: GetNicegramOnboardingStatusUseCase? = null
    var getSpecialOfferUseCase: GetSpecialOfferUseCase? = null
    var appSessionControlUseCase: AppSessionControlUseCase? = null
    var getSetPstStartedStatusUseCase: GetSetPstStartedStatusUseCase? = null
    lateinit var getChatPlacementsUseCase: GetChatPlacementsUseCase
    lateinit var aiChatConfigRepo: AiChatRemoteConfigRepo

    val pstConfig: AiChatRemoteConfigRepo.PstConfig? get() = aiChatConfigRepo.pstConfig

    fun getSpecialOffer(): SpecialOffersRepository.SpecialOffer? {
        if (!getSpecialOfferUseCase!!.haveSeenCurrentOffer() && getSpecialOfferUseCase!!.canShowSpecialOfferCurrentSession(
                appSessionControlUseCase!!.appSessionNumber
            )
        ) {
            return getSpecialOfferUseCase!!.specialOffer
        }

        return null
    }

    fun hasStartedPstOnce() = getSetPstStartedStatusUseCase?.hasStartedBotOnce == true

    fun findSpecialOffer(id: Int): SpecialOffersRepository.SpecialOffer? {
        return getSpecialOfferUseCase?.allOffers?.find { it.id == id }
    }

    fun getPossibleChatPlacements(isRestricted: Boolean, hasNgPremium: Boolean) =
        getChatPlacementsUseCase.invoke()
            .filter { placement ->
                val isChatTypeMatch = if (isRestricted) placement.showInRestrictedChat else placement.showInChat
                val isUserTypeMatch = if (hasNgPremium) placement.showToPremium else true // Предполагаем, что все могут видеть непремиум-контент
                isChatTypeMatch && isUserTypeMatch
            }

    val esimSplashData get() = aiChatConfigRepo.esimSplashData
}
