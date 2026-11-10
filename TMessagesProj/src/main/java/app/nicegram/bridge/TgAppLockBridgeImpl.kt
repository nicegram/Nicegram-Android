package app.nicegram.bridge

import android.app.Activity
import android.content.Intent
import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import app.nicegram.NicegramDoubleBottom
import com.appvillis.core_domain.bridge.TgAppLockBridge
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.FileLog
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.NotificationsController
import org.telegram.messenger.SharedConfig
import org.telegram.messenger.UserConfig
import org.telegram.ui.Components.PasscodeViewDialog
import org.telegram.ui.LaunchActivity
import java.util.WeakHashMap
import javax.inject.Inject

class TgAppLockBridgeImpl @Inject constructor() : TgAppLockBridge {

    private val dialogs = WeakHashMap<Activity, PasscodeViewDialog>()

    override fun isPasscodeSet(): Boolean = SharedConfig.passcodeHash.isNotEmpty()

    override fun isAppLocked(): Boolean = SharedConfig.appLocked

    override fun autoLockInSeconds(): Int = SharedConfig.autoLockIn

    override fun consumeLockDue(): Boolean = AndroidUtilities.needShowPasscode(true)

    override fun markPaused() {
        SharedConfig.lastPauseTime = if (isPasscodeSet()) (SystemClock.elapsedRealtime() / 1000).toInt() else 0
        SharedConfig.saveConfig()
    }

    override fun markResumed() {
        if (SharedConfig.lastPauseTime != 0) {
            SharedConfig.lastPauseTime = 0
            SharedConfig.saveConfig()
        }
    }

    override fun cancelTelegramAutoLock() {
        LaunchActivity.instance?.ngCancelPasscodeLock()
    }

    override fun isScreenCaptureBlocked(): Boolean = isPasscodeSet() && !SharedConfig.allowScreenCapture

    override fun showPasscode(activity: Activity, onUnlocked: (accountSwitched: Boolean) -> Unit) {
        val shown = dialogs[activity]
        if (shown != null && shown.isShowing && shown.passcodeView.visibility == View.VISIBLE) {
            shown.passcodeView.onResume()
            return
        }
        if (activity.isFinishing || activity.isDestroyed) return
        val dialog = TaskPasscodeDialog(activity)
        dialogs[activity] = dialog
        dialog.passcodeView.ngPromptOnHostActivity = true
        SharedConfig.appLocked = true
        dialog.show()
        dialog.passcodeView.onShow(true, false, -1, -1, null, null)
        SharedConfig.isWaitingForPasscodeEnter = true
        dialog.passcodeView.setDelegate { view ->
            val accountSwitched = switchAwayFromHiddenAccount()
            SharedConfig.isWaitingForPasscodeEnter = false
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.passcodeDismissed, view)
            showNotifications()
            onUnlocked(accountSwitched)
        }
        showNotifications()
    }

    override fun handOverToTelegram(activity: Activity) {
        if (liveLaunchActivity() == null) {
            activity.startActivity(Intent(activity, LaunchActivity::class.java))
        }
    }

    override fun onActivityPaused(activity: Activity) {
        dialogs[activity]?.takeIf { it.isShowing }?.passcodeView?.onPause()
    }

    override fun release(activity: Activity) {
        dialogs.remove(activity)
    }

    private fun switchAwayFromHiddenAccount(): Boolean {
        if (!NicegramDoubleBottom.needToHideAccount(UserConfig.getInstance(UserConfig.selectedAccount).clientUserId)) return false
        for (account in 0 until UserConfig.MAX_ACCOUNT_COUNT) {
            if (!NicegramDoubleBottom.needToHideAccount(UserConfig.getInstance(account).clientUserId)) {
                selectAccount(account)
                break
            }
        }
        return true
    }

    private fun selectAccount(account: Int) {
        val launchActivity = liveLaunchActivity()
        if (launchActivity != null) {
            launchActivity.switchToAccount(account, true)
            return
        }
        if (account == UserConfig.selectedAccount || !UserConfig.isValidAccount(account)) return
        UserConfig.selectedAccount = account
        UserConfig.getInstance(0).saveConfig(false)
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.activeAccountChanged, account)
    }

    private fun liveLaunchActivity(): LaunchActivity? =
        LaunchActivity.instance?.takeUnless { it.isFinishing || it.isDestroyed }

    private fun showNotifications() {
        try {
            NotificationsController.getInstance(UserConfig.selectedAccount).showNotifications()
        } catch (e: Exception) {
            FileLog.e(e)
        }
    }

    private class TaskPasscodeDialog(private val owner: Activity) : PasscodeViewDialog(owner) {

        override fun onBackPressed() {
            if (passcodeView.onBackPressed()) owner.moveTaskToBack(true)
        }

        override fun dismiss() {
            if (passcodeView.onBackPressed()) owner.moveTaskToBack(true)
        }

        override fun dispatchKeyEvent(event: KeyEvent): Boolean {
            if (event.keyCode == KeyEvent.KEYCODE_BACK && event.repeatCount == 0) {
                if (event.action == KeyEvent.ACTION_UP) onBackPressed()
                return true
            }
            return super.dispatchKeyEvent(event)
        }
    }
}
