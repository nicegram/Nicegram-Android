package app.nicegram.bridge

import androidx.collection.forEach
import com.appvillis.feature_attention_economy.bridge.AttChatListPeersProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.telegram.messenger.ChatObject
import org.telegram.messenger.MessagesController
import org.telegram.messenger.UserConfig
import org.telegram.messenger.UserObject

class AttChatListPeersProviderImpl : AttChatListPeersProvider {
    private val _usernamesFlow = MutableStateFlow<List<UsernameEntry>>(listOf())

    override fun updateUsernamesFlow() {
        val result = mutableListOf<UsernameEntry>()
        val mc = MessagesController.getInstance(UserConfig.selectedAccount)

        mc.dialogs_dict.forEach { i, it ->
            val isChannel = ChatObject.isChannel(mc.getChat(-it.id))
            val isBot = UserObject.isBot(mc.getUser(it.id))
            val user = mc.getUser(it.id)
            val chat = mc.getChat(-it.id)
            val publicUsername = if (user != null) UserObject.getPublicUsername(user) else if (chat != null) ChatObject.getPublicUsername(chat) else null

            if (!publicUsername.isNullOrEmpty()) {
                result.add(UsernameEntry(publicUsername, isBot, isChannel))
            }

        }
        if (result.size <= _usernamesFlow.value.size && mc.dialogsLoaded) _usernamesFlow.value = result
    }

    override fun usernamesFlow(categories: List<AttChatListPeersProvider.ChatListPeersProviderCategory>): Flow<List<String>> {
        return _usernamesFlow.map {
            it.filter { entry ->
                (entry.isBot && categories.contains(AttChatListPeersProvider.ChatListPeersProviderCategory.Bots)) || entry.isChannel && (categories.contains(
                    AttChatListPeersProvider.ChatListPeersProviderCategory.Groups
                ) || categories.contains(AttChatListPeersProvider.ChatListPeersProviderCategory.Channels))
            }.map { entry -> entry.username }
        }
    }

    data class UsernameEntry(val username: String, val isBot: Boolean, val isChannel: Boolean)
}