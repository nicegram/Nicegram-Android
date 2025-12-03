package app.nicegram.bridge

import com.appvillis.nicegram_wallet.module_bridge.ContactMessageSender
import org.telegram.messenger.SendMessagesHelper
import org.telegram.messenger.SendMessagesHelper.SendMessageParams
import org.telegram.messenger.UserConfig

class ContactMessageSenderImpl : ContactMessageSender {
    override fun sendMessage(text: String, id: String) {
        val currentAccount = UserConfig.selectedAccount
        val params = SendMessageParams.of(
            text,
            id.toLong(),
            null,
            null,
            null,
            true,
            arrayListOf(),
            null,
            null,
            true,
            0,
            0,
            null,
            false
        )
        SendMessagesHelper.getInstance(currentAccount).sendMessage(params)
    }
}
