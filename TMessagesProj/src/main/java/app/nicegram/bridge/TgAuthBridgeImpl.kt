package app.nicegram.bridge

import com.appvillis.bridges.user.bridges.TgAuthBridge
import org.telegram.messenger.UserConfig

class TgAuthBridgeImpl : TgAuthBridge {
    override val isLoggedInInTg: Boolean
        get() = UserConfig.getInstance(UserConfig.selectedAccount).isClientActivated

    override val telegramId: Long
        get() = UserConfig.getInstance(UserConfig.selectedAccount).clientUserId
}