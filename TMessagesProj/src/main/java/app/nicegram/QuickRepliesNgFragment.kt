package app.nicegram

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import app.nicegram.QuickRepliesHelper.getSavedReplies
import app.nicegram.QuickRepliesHelper.saveReplies
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import org.telegram.ui.ActionBar.ActionBar.ActionBarMenuOnItemClick
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.LayoutHelper

class QuickRepliesNgFragment : BaseFragment() {

    override fun createView(context: Context): View {

        val quickRepliesView = QuickRepliesView(context).apply {
            setQuickReplies(
                getSavedReplies(currentAccount)
                    .map { QuickRepliesView.QuickReply(0, it) }) { replies ->
                saveReplies(
                    replies.filter { !it.text.isNullOrEmpty() }.map { it.text!! },
                    currentAccount
                )
            }
        }

        actionBar.setBackButtonImage(R.drawable.ic_ab_back)
        actionBar.setAllowOverlayTitle(true)
        actionBar.setTitle(LocaleController.getString("QuickReplies"))
        actionBar.setActionBarMenuOnItemClick(object : ActionBarMenuOnItemClick() {
            override fun onItemClick(id: Int) {
                if (id == -1) {
                    finishFragment()
                }
            }
        })

        fragmentView = FrameLayout(context)
        val frameLayout = fragmentView as FrameLayout
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray))

        frameLayout.addView(quickRepliesView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))

        return fragmentView
    }
}