package app.nicegram.bridge

import com.appvillis.core_domain.bridge.TgUserBridge
import java.util.Locale
import javax.inject.Inject
import org.telegram.messenger.MessagesController
import org.telegram.messenger.UserConfig

class TgUserBridgeImpl @Inject constructor() : TgUserBridge {
    override val isLoggedInInTg: Boolean
        get() = UserConfig.getInstance(UserConfig.selectedAccount).isClientActivated

    override val telegramId: Long
        get() = UserConfig.getInstance(UserConfig.selectedAccount).clientUserId

    override val isTgPremium: Boolean
        get() = runCatching {
            UserConfig.getInstance(UserConfig.selectedAccount).currentUser?.premium == true
        }.getOrDefault(false)

    override val birthdate: String?
        get() = runCatching {
            val account = UserConfig.selectedAccount
            val userId = UserConfig.getInstance(account).clientUserId
            val birthday = MessagesController.getInstance(account)
                .getUserFull(userId)?.birthday ?: return@runCatching null
            if (birthday.year == 0) return@runCatching null
            String.format(Locale.US, "%04d-%02d-%02d", birthday.year, birthday.month, birthday.day)
        }.getOrNull()
}
