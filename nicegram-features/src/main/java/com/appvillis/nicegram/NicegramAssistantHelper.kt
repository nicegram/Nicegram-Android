package com.appvillis.nicegram

import android.content.Context
import com.appvillis.feature_nicegram_assistant.domain.SpecialOffersRepository
import dagger.hilt.EntryPoints

object NicegramAssistantHelper {
    private fun entryPoint(context: Context) = EntryPoints.get(context.applicationContext, NicegramAssistantEntryPoint::class.java)

    fun getSpecialOffer(context: Context): SpecialOffersRepository.SpecialOffer? {
        val ep = entryPoint(context)
        val getSpecialOfferUseCase = ep.getSpecialOfferUseCase()
        val appSessionControlUseCase = ep.appSessionControlUseCase()

        if (!getSpecialOfferUseCase.haveSeenCurrentOffer() && getSpecialOfferUseCase.canShowSpecialOfferCurrentSession(
                appSessionControlUseCase.appSessionNumber
            )
        ) {
            return getSpecialOfferUseCase.specialOffer
        }

        return null
    }

    fun findSpecialOffer(context: Context, id: Int): SpecialOffersRepository.SpecialOffer? {
        val ep = entryPoint(context)
        val getSpecialOfferUseCase = ep.getSpecialOfferUseCase()

        return getSpecialOfferUseCase.allOffers.find { it.id == id }
    }

    fun getPossibleChatPlacements(context: Context, isRestricted: Boolean, hasNgPremium: Boolean) =
        entryPoint(context).getChatPlacementsUseCase().invoke()
            .filter { placement ->
                val isChatTypeMatch = if (isRestricted) placement.showInRestrictedChat else placement.showInChat
                val isUserTypeMatch =
                    if (hasNgPremium) placement.showToPremium else true // Предполагаем, что все могут видеть непремиум-контент
                isChatTypeMatch && isUserTypeMatch
            }

    fun getEsimSplashData(context: Context) = entryPoint(context).aiChatRemoteConfigRepo().esimSplashData

    fun shouldShowAvatarsWelcome(context: Context) = !entryPoint(context).avatarsOnboardingUseCase().hasSeenWelcome
    fun hasGeneratedAvatar(context: Context) = entryPoint(context).getAvatarsUseCase().hasAnyAvatar
}
