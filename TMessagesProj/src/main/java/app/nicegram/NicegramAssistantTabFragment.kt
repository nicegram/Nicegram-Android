package app.nicegram

import android.content.Context
import android.view.MotionEvent
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.appvillis.feature_nicegram_assistant.api.AssistantTabView
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.NotificationCenter
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.DialogsActivity
import org.telegram.ui.MainTabsActivity

class NicegramAssistantTabFragment : BaseFragment(), MainTabsActivity.TabFragmentDelegate,
    NotificationCenter.NotificationCenterDelegate {

    private var assistantView: AssistantTabView? = null

    override fun onFragmentCreate(): Boolean {
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.didSetNewTheme)
        return super.onFragmentCreate()
    }

    override fun createActionBar(context: Context): ActionBar? = null

    override fun createView(context: Context): View {
        hasOwnBackground = true
        val view = AssistantTabView(context)
        view.setDarkTheme(Theme.isCurrentThemeDark())
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val systemInsets = AndroidUtilities.getDefaultWindowInsets(insets, false)
            view.setContentInsets(
                systemInsets.top,
                systemInsets.bottom + AndroidUtilities.dp(DialogsActivity.MAIN_TABS_HEIGHT_WITH_MARGINS.toFloat()),
            )
            WindowInsetsCompat.CONSUMED
        }
        assistantView = view
        fragmentView = view
        return view
    }

    override fun canParentTabsSlide(ev: MotionEvent?, forward: Boolean): Boolean = true

    override fun isSupportEdgeToEdge(): Boolean = true

    override fun isLightStatusBar(): Boolean = !Theme.isCurrentThemeDark()

    override fun onResume() {
        super.onResume()
        assistantView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        assistantView?.onPause()
    }

    override fun didReceivedNotification(id: Int, account: Int, vararg args: Any?) {
        if (id == NotificationCenter.didSetNewTheme) {
            assistantView?.setDarkTheme(Theme.isCurrentThemeDark())
        }
    }

    override fun clearViews() {
        destroyAssistantView()
        super.clearViews()
    }

    override fun onFragmentDestroy() {
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.didSetNewTheme)
        destroyAssistantView()
        super.onFragmentDestroy()
    }

    private fun destroyAssistantView() {
        assistantView?.let {
            it.onPause()
            it.destroy()
        }
        assistantView = null
    }
}
