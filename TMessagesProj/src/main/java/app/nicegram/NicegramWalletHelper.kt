package app.nicegram

import android.content.Context
import com.appvillis.assistant_core.MainActivity
import com.appvillis.nicegram_wallet.wallet_scanqr.QrProcessResult
import com.appvillis.nicegram_wallet.wallet_scanqr.QrResultEmitter
import com.appvillis.nicegram_wallet.wallet_security.domain.VerificationManager
import com.appvillis.nicegram_wallet.wallet_storage.domain.GetCurrentWalletUseCase
import com.appvillis.nicegram_wallet.wallet_tonconnect.domain.TcDeeplinkManager
import com.appvillis.rep_user.domain.GetUserStatusUseCase
import kotlinx.coroutines.CoroutineScope

object NicegramWalletHelper {
    var tcDeeplinkManager: TcDeeplinkManager? = null
    var getUserStatusUseCase: GetUserStatusUseCase? = null
    var getCurrentWalletUseCase: GetCurrentWalletUseCase? = null
    var verificationManager: VerificationManager? = null
    var appScope: CoroutineScope? = null
    var qrResultEmitter: QrResultEmitter? = null

    fun isLoggedInAndHasWallet() =
        getUserStatusUseCase?.isUserLoggedIn == true && getCurrentWalletUseCase?.currentWallet != null

    fun launchWalletIfPossible(context: Context, telegramId: Long) {
        val getUserStatusUseCase = getUserStatusUseCase ?: return
        val getCurrentWalletUseCase = getCurrentWalletUseCase ?: return

        if (getUserStatusUseCase.isUserLoggedIn && getCurrentWalletUseCase.currentWallet != null) {
            appScope?.let { appScope ->
                verificationManager?.doActionAfterVerification(scope = appScope, action = {
                    MainActivity.launchWalletStart(context, telegramId)
                })
            }
            MainActivity.launchPassCode(context, telegramId)
        } else {
            MainActivity.launchAssistant(context, telegramId)
        }
    }

    fun openWcLink(wcLink: String, context: Context, telegramId: Long) {
        val getUserStatusUseCase = getUserStatusUseCase ?: return
        val getCurrentWalletUseCase = getCurrentWalletUseCase ?: return

        if (getUserStatusUseCase.isUserLoggedIn && getCurrentWalletUseCase.currentWallet != null) {
            qrResultEmitter?.emitResult(QrProcessResult.WcLink(wcLink))
        } else {
            MainActivity.launchAssistant(context, telegramId)
        }
    }
}