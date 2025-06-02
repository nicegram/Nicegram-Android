package com.appvillis.nicegram

import com.appvillis.core_network.ApiService
import com.appvillis.core_resources.domain.TgResourceProvider
import com.appvillis.feature_ai_chat.domain.AiChatRemoteConfigRepo
import com.appvillis.feature_ai_chat.domain.ClearDataUseCase
import com.appvillis.feature_ai_chat.domain.UseResultManager
import com.appvillis.feature_ai_chat.domain.usecases.GetBalanceTopUpRequestUseCase
import com.appvillis.feature_ai_chat.domain.usecases.GetChatCommandsUseCase
import com.appvillis.feature_analytics.domain.AnalyticsManager
import com.appvillis.feature_attention_economy.domain.usecases.ClaimAdsUseCase
import com.appvillis.feature_attention_economy.domain.usecases.GetOngoingActionsUseCase
import com.appvillis.feature_avatar_generator.domain.usecases.AvatarsOnboardingUseCase
import com.appvillis.feature_avatar_generator.domain.usecases.GetAvatarsUseCase
import com.appvillis.feature_nicegram_assistant.domain.GetNicegramOnboardingStatusUseCase
import com.appvillis.feature_nicegram_assistant.domain.GetSpecialOfferUseCase
import com.appvillis.feature_nicegram_billing.domain.BillingManager
import com.appvillis.feature_nicegram_billing.domain.RequestInAppsUseCase
import com.appvillis.feature_nicegram_client.domain.CollectGroupInfoUseCase
import com.appvillis.feature_nicegram_client.domain.NgClientRemoteConfigRepo
import com.appvillis.feature_nicegram_client.domain.NgRevLoginUseCase
import com.appvillis.feature_nicegram_client.domain.NicegramSessionCounter
import com.appvillis.feature_pump_ads.domain.usecases.GetPumpAdsConfigUseCase
import com.appvillis.feature_pump_ads.domain.usecases.GetSettingsUseCase
import com.appvillis.feature_pump_ads.domain.usecases.UpdateSettingsUseCase
import com.appvillis.nicegram_wallet.module_bridge.InChatResultManager
import com.appvillis.nicegram_wallet.wallet_dapps.domain.BrowserResponseManager
import com.appvillis.nicegram_wallet.wallet_dapps.domain.TgBrowserBridgeFactory
import com.appvillis.nicegram_wallet.wallet_scanqr.QrResultEmitter
import com.appvillis.nicegram_wallet.wallet_security.domain.VerificationManager
import com.appvillis.nicegram_wallet.wallet_storage.domain.GetCurrentWalletUseCase
import com.appvillis.nicegram_wallet.wallet_tonconnect.domain.TcDeeplinkManager
import com.appvillis.nicegram_wallet.wallet_tonconnect.domain.WalletPopupActivityLauncher
import com.appvillis.rep_placements.domain.GetChatPlacementsUseCase
import com.appvillis.rep_placements.domain.GetPinChatsPlacementsUseCase
import com.appvillis.rep_placements.domain.GetPinPlacementsStatusUseCase
import com.appvillis.rep_placements.domain.HidePlacementUseCase
import com.appvillis.rep_user.domain.AppSessionControlUseCase
import com.appvillis.rep_user.domain.GetUserStatusUseCase
import com.appvillis.rep_user.domain.UserRepository
import com.appvillis.rep_user_actions.domain.usecases.SaveUserActionUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope

@EntryPoint
@InstallIn(SingletonComponent::class)
interface NicegramAssistantEntryPoint {
    //region common
    fun appScope(): CoroutineScope
    fun tgResourceProvider(): TgResourceProvider
    fun getUserStatusUseCase(): GetUserStatusUseCase
    fun nicegramSessionCounter(): NicegramSessionCounter
    fun appSessionControlUseCase(): AppSessionControlUseCase
    fun getNicegramOnboardingStatusUseCase(): GetNicegramOnboardingStatusUseCase
    fun analyticsManager(): AnalyticsManager
    fun getChatPlacementsUseCase(): GetChatPlacementsUseCase
    fun getPinChatsPlacementsUseCase(): GetPinChatsPlacementsUseCase
    fun getPinPlacementsStatusUseCase(): GetPinPlacementsStatusUseCase
    fun hidePlacementUseCase(): HidePlacementUseCase
    fun userRepository(): UserRepository
    fun collectGroupInfoUseCase(): CollectGroupInfoUseCase
    fun ngClientRemoteConfigRepo(): NgClientRemoteConfigRepo
    fun apiService(): ApiService
    fun ngRevLoginUseCase(): NgRevLoginUseCase
    fun getPumpAdsConfigUseCase(): GetPumpAdsConfigUseCase
    fun getPumpSettingsUseCase(): GetSettingsUseCase
    fun updatePumpSettingsUseCase(): UpdateSettingsUseCase
    fun saveUserActionUseCase(): SaveUserActionUseCase
    fun getOngoingActionsUseCase(): GetOngoingActionsUseCase
    fun claimAdsUseCase(): ClaimAdsUseCase
    // end region

    // region special offer
    fun getSpecialOfferUseCase(): GetSpecialOfferUseCase
    // end region

    // region avatars
    fun avatarsOnboardingUseCase(): AvatarsOnboardingUseCase
    fun getAvatarsUseCase(): GetAvatarsUseCase
    // end region

    // region billing
    fun billingManager(): BillingManager
    fun requestInAppsUseCase(): RequestInAppsUseCase
    fun getBalanceTopUpRequestUseCase(): GetBalanceTopUpRequestUseCase
    // end region

    // region wallet
    fun getCurrentWalletUseCase(): GetCurrentWalletUseCase
    fun tcDeeplinkManager(): TcDeeplinkManager
    fun verificationManager(): VerificationManager
    fun qrResultEmitter(): QrResultEmitter
    fun inChatResultManager(): InChatResultManager
    fun tgBrowserBridgeFactory(): TgBrowserBridgeFactory
    fun browserResponseManager(): BrowserResponseManager
    fun walletPopupActivityLauncher(): WalletPopupActivityLauncher
    // end region

    // region ai
    fun getChatCommandsUseCase(): GetChatCommandsUseCase
    fun clearDataUseCase(): ClearDataUseCase
    fun useResultManager(): UseResultManager
    fun aiChatRemoteConfigRepo(): AiChatRemoteConfigRepo
    // end region
}
