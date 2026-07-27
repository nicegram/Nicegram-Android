package app.nicegram.bridge.att

import com.appvillis.feature_attention_economy.bridge.AttPeerUsernameResolver
import org.telegram.messenger.ChatObject
import org.telegram.messenger.MessagesController
import org.telegram.messenger.UserConfig
import org.telegram.messenger.UserObject

class AttPeerUsernameResolverImpl : AttPeerUsernameResolver {
    override fun getPublicUsername(dialogId: Long): String? {
        val mc = MessagesController.getInstance(UserConfig.selectedAccount)

        mc.getUser(dialogId)?.let { user ->
            return UserObject.getPublicUsername(user)?.takeIf { it.isNotEmpty() }
        }

        mc.getChat(-dialogId)?.let { chat ->
            return ChatObject.getPublicUsername(chat)?.takeIf { it.isNotEmpty() }
        }

        return null
    }
}
