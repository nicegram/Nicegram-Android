package app.nicegram

import android.content.Context
import android.os.Parcelable
import com.appvillis.assistant_core.navigation.DeeplinkRouteMapper
import com.appvillis.assistant_core.navigation.NavigatorEntryPoint
import com.appvillis.assistant_core.navigation.link.NicegramLink
import com.appvillis.assistant_core.navigation.telegram.ResultCallback
import com.appvillis.assistant_core.navigation.telegram.ResultSubscription
import com.appvillis.core_navigation.AccountExportRoute
import com.appvillis.core_navigation.AiChatRoute
import com.appvillis.core_navigation.AssistantRoute
import com.appvillis.core_navigation.BillingRoute
import com.appvillis.core_navigation.NavMode
import com.appvillis.core_navigation.Route
import com.appvillis.core_navigation.TelegramSessionRoute
import com.appvillis.core_navigation.args.AiChatUseResult
import com.appvillis.core_navigation.args.EsimSplashArgs
import com.appvillis.core_navigation.args.ExportAccountsResult
import com.appvillis.core_navigation.args.LogoutConfirmResult
import com.appvillis.feature_account_export.domain.Account
import com.appvillis.feature_account_export.navigation.toArgs
import com.appvillis.feature_account_export.navigation.toDomain
import com.appvillis.feature_ai_chat.domain.entry.EsimSplashData
import com.appvillis.feature_gods_eye.GodsEyeEntryPoint
import com.appvillis.feature_nicegram_billing.presentation.PremiumSource
import dagger.hilt.EntryPoints
import dagger.hilt.android.EntryPointAccessors
import timber.log.Timber

object NgNavigation {

    private fun ep(context: Context) =
        EntryPointAccessors.fromApplication(context.applicationContext, NavigatorEntryPoint::class.java)

    @JvmStatic
    fun open(context: Context?, route: Route) {
        if (context == null) {
            Timber.w("NgNavigation.open: no context, dropping $route")
            return
        }
        ep(context).sourcedNavigator().navigate(context, route, NavMode.NewRoot)
    }

    @JvmStatic
    fun openDeeplink(context: Context?, url: String, restored: Boolean): Boolean {
        if (context == null) {
            Timber.w("NgNavigation.openDeeplink: no context, dropping $url")
            return false
        }
        return ep(context).deeplinkOpener().open(context, url, restored)
    }

    @JvmStatic
    fun openAssistantHome(context: Context?) = open(context, AssistantRoute.Home())

    @JvmStatic
    fun afterPopupClosed(context: Context, action: Runnable) {
        ep(context).popupClosure().afterPopupClosed { action.run() }
    }

    @JvmStatic
    fun deeplinkTarget(url: String): String? = DeeplinkRouteMapper.target(url)

    @JvmStatic
    fun unwrapLink(url: String): String? = NicegramLink.unwrap(url)

    @JvmStatic
    @JvmOverloads
    fun <V : Parcelable> openForResult(
        context: Context?,
        route: Route,
        type: Class<V>,
        onValue: ResultCallback<V>,
        onDismissed: Runnable? = null,
    ): ResultSubscription? = openForResult(context, route, type, keepAlive = false, onValue, onDismissed)

    private fun <V : Parcelable> openForResult(
        context: Context?,
        route: Route,
        type: Class<V>,
        keepAlive: Boolean,
        onValue: ResultCallback<V>,
        onDismissed: Runnable?,
    ): ResultSubscription? {
        if (context == null) {
            Timber.w("NgNavigation.openForResult: no context, dropping $route")
            return null
        }
        val subscription = ep(context).resultCallbacks().subscribe(
            requestKey = route.requestKey,
            type = type,
            keepAlive = keepAlive,
            onValue = onValue,
            onDismissed = onDismissed,
        )
        open(context, route)
        return subscription
    }

    @JvmStatic
    fun openPremium(context: Context?) {
        open(context, BillingRoute.PremiumScreen(PremiumSource.DEFAULT.value))
    }

    @JvmStatic
    fun openTelegramSession(context: Context?, source: String) = open(context, TelegramSessionRoute.Shop(source))

    @JvmStatic
    fun openSpecialOffer(context: Context?, url: String, offerId: Int) {
        open(context, AssistantRoute.WebViewPopup(url, offerId))
    }

    @JvmStatic
    fun openWebViewPopup(context: Context?, url: String) {
        open(context, AssistantRoute.WebViewPopup(url))
    }

    @JvmStatic
    fun openEsimSplash(context: Context?, coupon: EsimSplashData.Coupon, url: String) {
        open(context, AssistantRoute.EsimSplash(EsimSplashArgs(coupon.coupon, "${coupon.bonusUsd}$", url)))
    }

    @JvmStatic
    fun openGodsEye(context: Context?, telegramId: Long) {
        if (context == null) {
            Timber.w("NgNavigation.openGodsEye: no context, dropping $telegramId")
            return
        }
        val resolver = EntryPoints.get(context.applicationContext, GodsEyeEntryPoint::class.java).godsEyeRouteResolver()
        open(context, resolver.routeFor(telegramId))
    }

    @JvmStatic
    fun openAiChatForUse(
        context: Context?,
        onText: ResultCallback<String>,
        onImage: ResultCallback<String>,
    ): ResultSubscription? = openForResult(
        context,
        AiChatRoute.Chat(AiChatRoute.USE_RESULT_KEY),
        AiChatUseResult::class.java,
        keepAlive = true,
        onValue = { result ->
            when (result) {
                is AiChatUseResult.Text -> onText.onResult(result.text)
                is AiChatUseResult.Image -> onImage.onResult(result.path)
            }
        },
        onDismissed = null,
    )

    @JvmStatic
    fun openExportAccounts(
        context: Context?,
        accounts: List<Account>,
        importMode: Boolean,
        onSelected: ResultCallback<List<Account>>,
    ): ResultSubscription? = openForResult(
        context,
        AccountExportRoute.Export(accounts.map { it.toArgs() }, importMode),
        ExportAccountsResult::class.java,
        keepAlive = true,
        onValue = { result -> onSelected.onResult(result.selected.map { it.toDomain() }) },
        onDismissed = null,
    )

    @JvmStatic
    fun openLogoutConfirm(context: Context?, onDeleteThis: Runnable, onDeleteAll: Runnable) {
        openForResult(
            context,
            AccountExportRoute.LogoutConfirm(),
            LogoutConfirmResult::class.java,
            { result ->
                when (result) {
                    LogoutConfirmResult.DeleteThis -> onDeleteThis.run()
                    LogoutConfirmResult.DeleteAll -> onDeleteAll.run()
                }
            },
        )
    }
}
