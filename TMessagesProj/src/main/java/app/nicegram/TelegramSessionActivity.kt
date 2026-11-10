package app.nicegram

import android.content.Context
import android.view.MotionEvent
import android.view.View
import com.appvillis.assistant_core.navigation.NavigatorEntryPoint
import com.appvillis.assistant_core.navigation.host.TelegramFragmentHost
import com.appvillis.core_navigation.TelegramSessionRoute
import dagger.hilt.android.EntryPointAccessors
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.ActionBar.Theme

class TelegramSessionActivity(private val source: String) : BaseFragment() {

    private var host: TelegramFragmentHost? = null

    override fun createActionBar(context: Context): ActionBar? = null

    override fun createView(context: Context): View {
        val fragmentHost = host ?: EntryPointAccessors
            .fromApplication(context.applicationContext, NavigatorEntryPoint::class.java)
            .telegramFragmentHostFactory()
            .create(
                activity = parentActivity,
                root = TelegramSessionRoute.Shop(source),
                darkTheme = Theme.isCurrentThemeDark(),
                statusBarHeightPx = AndroidUtilities.statusBarHeight,
                onClose = { finishFragment() },
            )
            .also { host = it }
        fragmentView = fragmentHost.createView(context)
        return fragmentView
    }

    override fun onResume() {
        super.onResume()
        host?.onResume()
    }

    override fun onPause() {
        super.onPause()
        host?.onPause()
    }

    override fun onFragmentDestroy() {
        host?.onDestroy()
        super.onFragmentDestroy()
    }

    override fun onBackPressed(invoked: Boolean): Boolean =
        super.onBackPressed(invoked) && (host?.onBackPressed(invoked) ?: true)

    override fun isSwipeBackEnabled(event: MotionEvent?): Boolean = host?.isSwipeBackEnabled ?: true

    override fun isLightStatusBar(): Boolean = !Theme.isCurrentThemeDark()
}
