package app.nicegram

import android.app.Activity
import com.appvillis.assistant_core.MainActivity
import com.appvillis.nicegram_wallet.wallet_tonconnect.domain.TcDeeplinkManager
import com.appvillis.rep_user.domain.GetUserStatusUseCase

object NicegramWalletHelper {
    var tcDeeplinkManager: TcDeeplinkManager? = null
    var getUserStatusUseCase: GetUserStatusUseCase? = null

    fun launchWallet(activity: Activity, telegramId: Long) {
        val getUserStatusUseCase = getUserStatusUseCase ?: return

        if (getUserStatusUseCase.isUserLoggedIn) {
            MainActivity.launchWalletStart(activity, telegramId)
        } else {
            MainActivity.launchAssistant(activity, telegramId)
        }
    }
}