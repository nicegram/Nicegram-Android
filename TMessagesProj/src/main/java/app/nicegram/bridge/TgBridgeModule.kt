package app.nicegram.bridge

import app.nicegram.bridge.att.AttChatListPeersProviderImpl
import app.nicegram.bridge.att.AttPeerUsernameResolverImpl
import com.appvillis.core_domain.bridge.TgUserBridge
import com.appvillis.core_network.UserLocaleProvider
import com.appvillis.core_ui.domain.TgImagesLoader
import com.appvillis.feature_attention_economy.bridge.AttChatListPeersProvider
import com.appvillis.feature_attention_economy.bridge.AttPeerUsernameResolver
import com.appvillis.feature_auth.domain.TelegramBotBridge
import com.appvillis.feature_keywords.domain.KeywordsSearchRetriever
import com.appvillis.feature_telegram_session.api.TgLoginBridge
import com.appvillis.feature_user_activities.domain.UserCommonGroupsMessagesRetriever
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.telegram.messenger.LocaleController
import java.util.Locale
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TgBridgeModule {
    @Provides
    @Singleton
    fun provideUserLocaleProvider(): UserLocaleProvider = object : UserLocaleProvider {
        override val lang: String?
            get() = try {
                LocaleController.getInstance().currentLocale?.language
            } catch (e: Exception) {
                Locale.getDefault().language
            } catch (e: UnsatisfiedLinkError) {
                Locale.getDefault().language
            }
    }

    @Provides
    @Singleton
    fun provideTelegramBotBridge(): TelegramBotBridge = TelegramBotBridgeImpl()

    @Provides
    @Singleton
    fun provideAttChatListPeersProvider(): AttChatListPeersProvider = AttChatListPeersProviderImpl()

    @Provides
    @Singleton
    fun provideAttPeerUsernameResolver(): AttPeerUsernameResolver = AttPeerUsernameResolverImpl()

    @Provides
    @Singleton
    fun provideKeywordsSearchRetriever(): KeywordsSearchRetriever = KeywordsSearchRetrieverImpl()

    @Provides
    @Singleton
    fun provideTgImagesLoader(): TgImagesLoader = TgImagesLoaderImpl()

    @Provides
    @Singleton
    fun provideUserCommonGroupsMessagesRetriever(): UserCommonGroupsMessagesRetriever =
        UserCommonGroupsMessagesRetrieverImpl()

    @Provides
    @Singleton
    fun provideTgLoginBridge(): TgLoginBridge = TgLoginBridgeImpl()
}

@Module
@InstallIn(SingletonComponent::class)
interface TgBridgeBindsModule {

    @Binds
    @Singleton
    fun bindTgUserBridge(impl: TgUserBridgeImpl): TgUserBridge
}