package app.nicegram.bridge

import app.nicegram.domain.usecases.PrepareMessagesUseCase
import com.appvillis.feature_attention_economy.bridge.AttChatListPeersProvider
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TgBridgeEntryPoint {
    fun attChatListPeersProvider(): AttChatListPeersProvider
    fun prepareMessagesUseCase(): PrepareMessagesUseCase
}
