package app.nicegram.bridge

import android.app.Activity
import android.content.Intent
import android.net.Uri
import app.nicegram.TelegramSessionActivity
import com.appvillis.core_domain.bridge.TgNavigationBridge
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.ApplicationLoader
import org.telegram.ui.LaunchActivity
import timber.log.Timber
import javax.inject.Inject

class TgNavigationBridgeImpl @Inject constructor() : TgNavigationBridge {
    override fun openChat(dialogId: Long, messageId: Int) {
        openNcgLink("ncg://tgOpenChat?msgId=$messageId&dialogId=$dialogId")
    }

    override fun openSettings() {
        openNcgLink("ncg://tgOpenSettings")
    }

    override fun openAssistantTab() {
        withLaunchActivity("openAssistantTab") { it.openNicegramAssistantTab() }
    }

    private fun openNcgLink(url: String) {
        val context = ApplicationLoader.applicationContext
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .setPackage(context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun withLaunchActivity(action: String, block: (LaunchActivity) -> Unit) {
        AndroidUtilities.runOnUIThread {
            val activity = LaunchActivity.instance
            if (activity == null) {
                Timber.w("TgNavigationBridge.$action: LaunchActivity is not alive, dropping")
            } else {
                block(activity)
            }
        }
    }

    override fun closeTopFragment() {
        AndroidUtilities.runOnUIThread { LaunchActivity.getLastFragment()?.finishFragment() }
    }

    override fun isTelegramActivity(activity: Activity): Boolean = activity is LaunchActivity

    override fun openTelegramSessionShop(source: String): Boolean {
        val fragment = LaunchActivity.getLastFragment() ?: return false
        AndroidUtilities.runOnUIThread { fragment.presentFragment(TelegramSessionActivity(source)) }
        return true
    }

    override fun openLink(url: String) {
        AndroidUtilities.runOnUIThread {
            val activity = LaunchActivity.instance
            if (activity != null && !activity.isFinishing) {
                activity.openNicegramLink(url)
            } else {
                val context = ApplicationLoader.applicationContext
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url), context, LaunchActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        }
    }
}
