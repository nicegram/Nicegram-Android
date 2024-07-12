package app.nicegram

import android.app.Activity
import com.appvillis.assistant_core.MainActivity
import com.appvillis.nicegram_wallet.wallet_storage.domain.GetCurrentWalletUseCase
import com.appvillis.nicegram_wallet.wallet_tonconnect.domain.TcDeeplinkManager
import com.appvillis.rep_user.domain.GetUserStatusUseCase

object NicegramWalletHelper {
    var tcDeeplinkManager: TcDeeplinkManager? = null
    var getUserStatusUseCase: GetUserStatusUseCase? = null
    var getCurrentWalletUseCase: GetCurrentWalletUseCase? = null

    fun isLoggedInAndHasWallet() = getUserStatusUseCase?.isUserLoggedIn == true && getCurrentWalletUseCase?.currentWallet != null

    fun launchWalletIfPossible(activity: Activity, telegramId: Long) {
        val getUserStatusUseCase = getUserStatusUseCase ?: return
        val getCurrentWalletUseCase = getCurrentWalletUseCase ?: return

        if (getUserStatusUseCase.isUserLoggedIn && getCurrentWalletUseCase.currentWallet != null) {
            MainActivity.launchWalletStart(activity, telegramId)
        } else {
            MainActivity.launchAssistant(activity, telegramId)
        }
    }
}