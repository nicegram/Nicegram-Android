package app.nicegram

import android.content.Context
import android.view.MotionEvent
import android.view.View
import com.appvillis.feature_telegram_session.api.TelegramSessionScreenController
import com.appvillis.feature_telegram_session.api.TelegramSessionSource
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.ActionBar.Theme

class TelegramSessionActivity(source: TelegramSessionSource) : BaseFragment() {

    private val controller = TelegramSessionScreenController(
        launchSource = source,
        darkTheme = Theme.isCurrentThemeDark(),
        statusBarHeightPx = AndroidUtilities.statusBarHeight,
        onClose = { finishFragment() },
    )

    override fun createActionBar(context: Context): ActionBar? = null

    override fun createView(context: Context): View {
        fragmentView = controller.createView(context)
        return fragmentView
    }

    override fun onResume() {
        super.onResume()
        controller.onResume()
    }

    override fun onPause() {
        super.onPause()
        controller.onPause()
    }

    override fun onFragmentDestroy() {
        controller.onDestroy()
        super.onFragmentDestroy()
    }

    override fun onBackPressed(invoked: Boolean): Boolean =
        super.onBackPressed(invoked) && controller.onBackPressed(invoked)

    override fun isSwipeBackEnabled(event: MotionEvent?): Boolean = controller.isSwipeBackEnabled

    override fun isLightStatusBar(): Boolean = !Theme.isCurrentThemeDark()
}
