package app.nicegram.bridge

import android.content.Context
import android.net.Uri
import com.appvillis.assistant_core.MainActivity
import com.appvillis.feature_analytics.domain.AnalyticsManager
import com.appvillis.nicegram_wallet.wallet_dapps.domain.GetDAppsUseCase
import com.appvillis.nicegram_wallet.wallet_remote_cofig.domain.GetWalletAvailabilityUseCase
import com.appvillis.nicegram_wallet.wallet_storage.domain.GetCurrentWalletUseCase
import com.appvillis.rep_user.domain.GetUserStatusUseCase
import timber.log.Timber
import javax.inject.Inject

class NicegramDeepLinksHelper @Inject constructor(
    private val getDAppsUseCase: GetDAppsUseCase,
    private val getUserStatusUseCase: GetUserStatusUseCase,
    private val getCurrentWalletUseCase: GetCurrentWalletUseCase,
    private val analyticsManager: AnalyticsManager,
    private val getWalletAvailabilityUseCase: GetWalletAvailabilityUseCase
) {
    companion object {
        var instance: NicegramDeepLinksHelper? = null

        private val ignoredDomains = listOf(
            "github.com",
            "nicegram.app",
            "t.me",
            "telegram.me"
        )

        private val ignoredSchemes = listOf(
            "tonsite"
        )
    }

    fun tryOpenUrl(url: String, context: Context, telegramId: Long): Boolean {
        Timber.d("tryOpenUrl $url")

        if (!getWalletAvailabilityUseCase.dAppsEnabled()) {
            Timber.d("DApp cannot be opened due to geo restrictions")
            return false
        }

        try {
            val uri = Uri.parse(url)
            val domainName = uri.host?.getSLDnTLDFromHost() ?: return false
            Timber.d("host is $domainName dApps size: ${getDAppsUseCase().value.data.size}")
            if (ignoredDomains.contains(domainName)) return false
            if (ignoredSchemes.contains(uri.scheme ?: "")) return false

            val dAppsMap =
                getDAppsUseCase().value.data.associateBy { (Uri.parse(it.url).host?.getSLDnTLDFromHost() ?: "") }

            if (getUserStatusUseCase.isUserLoggedIn && getCurrentWalletUseCase.currentWallet != null) {
                MainActivity.launchDApp(dAppsMap[domainName] ?: return false, context, telegramId)
            } else {
                if (dAppsMap[domainName] == null) return false

                MainActivity.launchAssistant(context, telegramId)
            }

            analyticsManager.logEvent("wallet_dapp_link_click")
            analyticsManager.logEvent("wallet_dapp_link_$domainName")

            return true
        } catch (e: Exception) {
            Timber.e(e)
            return false
        }
    }

    private fun String.getSLDnTLDFromHost() = split(".").takeLast(2).joinToString(".")
}