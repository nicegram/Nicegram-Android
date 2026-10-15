package com.appvillis.nicegram

import com.appvillis.core_domain.repository.user.UserRepository
import com.appvillis.core_domain.usecase.user.AppSessionControlUseCase
import com.appvillis.core_domain.usecase.user.FetchNicegramUserLoggedInStatusUseCase
import com.appvillis.core_ui.domain.TgResourceProvider
import com.appvillis.feature_ai_chat.domain.AiChatRemoteConfigRepo
import com.appvillis.feature_ai_chat.domain.ClearDataUseCase
import com.appvillis.feature_ai_chat.domain.UseResultManager
import com.appvillis.feature_attention_economy.domain.usecases.ClaimAdsUseCase
import com.appvillis.feature_attention_economy.domain.usecases.GetOngoingActionsUseCase
import com.appvillis.feature_auth.domain.CheckIfNeedToCompleteAutoLoginUseCase
import com.appvillis.feature_nicegram_assistant.domain.GetSpecialOfferUseCase
import com.appvillis.core_domain.BillingManager
import com.appvillis.feature_nicegram_client.domain.CollectGroupInfoUseCase
import com.appvillis.feature_nicegram_client.domain.IsReviewPhoneUseCase
import com.appvillis.feature_nicegram_client.domain.NicegramSessionCounter
import com.appvillis.core_domain.usecase.placement.GetChatPlacementsUseCase
import com.appvillis.core_common.DispatchersProvider
import com.appvillis.core_domain.usecase.telegramsession.IsNeedToShowTelegramSessionBackupUseCase
import com.appvillis.core_domain.usecase.telegramsession.IsSystemTelegramSessionUseCase
import com.appvillis.core_domain.usecase.telegramsession.IsTelegramSessionEnabledUseCase
import com.appvillis.core_domain.usecase.translate.TranslateTextUseCase
import com.appvillis.core_domain.usecase.placement.GetAllPinChatsPlacementsUseCase
import com.appvillis.core_domain.usecase.placement.IsPinChatsPlacementHiddenUseCase
import com.appvillis.core_domain.usecase.placement.SetPinChatsPlacementHiddenUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope

@EntryPoint
@InstallIn(SingletonComponent::class)
interface NicegramAssistantEntryPoint {
    //region common
    fun appScope(): CoroutineScope
    fun dispatchersProvider(): DispatchersProvider
    fun tgResourceProvider(): TgResourceProvider
    fun getUserStatusUseCase(): FetchNicegramUserLoggedInStatusUseCase
    fun nicegramSessionCounter(): NicegramSessionCounter
    fun appSessionControlUseCase(): AppSessionControlUseCase
    fun getChatPlacementsUseCase(): GetChatPlacementsUseCase
    fun getAllPinChatsPlacementsUseCase(): GetAllPinChatsPlacementsUseCase
    fun isPinChatsPlacementHiddenUseCase(): IsPinChatsPlacementHiddenUseCase
    fun setPinChatsPlacementHiddenUseCase(): SetPinChatsPlacementHiddenUseCase
    fun userRepository(): UserRepository
    fun collectGroupInfoUseCase(): CollectGroupInfoUseCase
    fun isReviewPhoneUseCase(): IsReviewPhoneUseCase
    fun getOngoingActionsUseCase(): GetOngoingActionsUseCase
    fun claimAdsUseCase(): ClaimAdsUseCase
    fun isTelegramSessionEnabledUseCase(): IsTelegramSessionEnabledUseCase
    fun isNeedToShowTelegramSessionBackupUseCase(): IsNeedToShowTelegramSessionBackupUseCase
    fun isSystemTelegramSessionUseCase(): IsSystemTelegramSessionUseCase
    // end region

    // region special offer
    fun getSpecialOfferUseCase(): GetSpecialOfferUseCase
    // end region

    // region billing
    fun billingManager(): BillingManager
    // end region

    // region ai
    fun clearDataUseCase(): ClearDataUseCase
    fun useResultManager(): UseResultManager
    fun aiChatRemoteConfigRepo(): AiChatRemoteConfigRepo
    // end region
    fun checkIfNeedToCompleteAutoLoginUseCase(): CheckIfNeedToCompleteAutoLoginUseCase
    fun translateTextUseCase(): TranslateTextUseCase
}
