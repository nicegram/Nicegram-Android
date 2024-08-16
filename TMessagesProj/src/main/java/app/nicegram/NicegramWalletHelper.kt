package app.nicegram

import android.content.Context
import com.appvillis.assistant_core.MainActivity
import com.appvillis.nicegram_wallet.wallet_security.domain.VerificationManager
import com.appvillis.nicegram_wallet.wallet_storage.domain.GetCurrentWalletUseCase
import com.appvillis.nicegram_wallet.wallet_tonconnect.domain.TcDeeplinkManager
import com.appvillis.rep_user.domain.GetUserStatusUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope

object NicegramWalletHelper {
    var tcDeeplinkManager: TcDeeplinkManager? = null
    var getUserStatusUseCase: GetUserStatusUseCase? = null
    var getCurrentWalletUseCase: GetCurrentWalletUseCase? = null
    var verificationManager: VerificationManager? = null
    var appScope: CoroutineScope? = null

    fun isLoggedInAndHasWallet() =
        getUserStatusUseCase?.isUserLoggedIn == true && getCurrentWalletUseCase?.currentWallet != null

    fun launchWalletIfPossible(context: Context, telegramId: Long) {
        val getUserStatusUseCase = getUserStatusUseCase ?: return
        val getCurrentWalletUseCase = getCurrentWalletUseCase ?: return

        if (getUserStatusUseCase.isUserLoggedIn && getCurrentWalletUseCase.currentWallet != null) {
            MainActivity.launchWalletStart(context, telegramId)
        } else {
            MainActivity.launchAssistant(context, telegramId)
        }
    }
}