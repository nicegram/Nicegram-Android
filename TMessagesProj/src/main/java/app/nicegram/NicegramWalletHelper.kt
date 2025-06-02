package app.nicegram

import android.content.Context
import com.appvillis.assistant_core.InChatMainActivity
import com.appvillis.assistant_core.MainActivity
import com.appvillis.nicegram.NicegramAssistantEntryPoint
import com.appvillis.nicegram_wallet.wallet_contacts.domain.WalletContact
import com.appvillis.nicegram_wallet.wallet_scanqr.QrProcessResult
import dagger.hilt.EntryPoints
import kotlinx.coroutines.delay
import org.telegram.messenger.ApplicationLoader

object NicegramWalletHelper {
    private fun entryPoint() = EntryPoints.get(ApplicationLoader.applicationContext, NicegramAssistantEntryPoint::class.java)

    fun getTcDeeplinkManager() = entryPoint().tcDeeplinkManager()

    fun isWalletPopupShowing(): Boolean {
        val ep = entryPoint()
        val walletPopupActivityLauncher = ep.walletPopupActivityLauncher()

        return walletPopupActivityLauncher.isWalletPopupShowing()
    }

    fun isLoggedInAndHasWallet(): Boolean {
        val ep = entryPoint()
        val getUserStatusUseCase = ep.getUserStatusUseCase()
        val getCurrentWalletUseCase = ep.getCurrentWalletUseCase()

        return getUserStatusUseCase.isUserLoggedIn && getCurrentWalletUseCase.currentWallet != null
    }

    fun launchWalletIfPossible(context: Context) {
        val ep = entryPoint()
        val getUserStatusUseCase = ep.getUserStatusUseCase()
        val getCurrentWalletUseCase = ep.getCurrentWalletUseCase()

        if (getUserStatusUseCase.isUserLoggedIn && getCurrentWalletUseCase.currentWallet != null) {
            ep.appScope().let { appScope ->
                ep.verificationManager().doActionAfterVerification(scope = appScope, action = {
                    delay(600)

                    MainActivity.launchWalletStart(context)
                })
            }
            MainActivity.launchPassCode(context)
        } else {
            WalletContact.closeInChatAfterWallet = true
            WalletContact.navigateToMainWalletAfterClose = true

            InChatMainActivity.launch(context, WalletContact.PREVIEW)
        }
    }

    fun openWcLink(wcLink: String, context: Context) {
        val ep = entryPoint()
        val getUserStatusUseCase = ep.getUserStatusUseCase()
        val getCurrentWalletUseCase = ep.getCurrentWalletUseCase()

        if (getUserStatusUseCase.isUserLoggedIn && getCurrentWalletUseCase.currentWallet != null) {
            ep.qrResultEmitter().emitResult(QrProcessResult.WcLink(wcLink))
        } else {
            MainActivity.launchAssistant(context)
        }
    }
}
