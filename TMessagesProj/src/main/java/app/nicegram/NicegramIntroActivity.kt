package app.nicegram

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import com.appvillis.assistant_core.navigation.telegram.ResultSubscription
import com.appvillis.core_navigation.OnboardingRoute
import com.appvillis.core_navigation.args.OnboardingResult
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.LoginActivity

class NicegramIntroActivity : BaseFragment() {

    private var blockActivityCreation = false

    private var onboardingSubscription: ResultSubscription? = null

    private var reopenPending = false

    private var destroyed = false

    override fun createView(context: Context): View {
        if (!blockActivityCreation) {
            openOnboarding()
        }

        return FrameLayout(parentActivity).apply {
            setBackgroundColor(Color.BLACK)
        }
    }

    private fun openOnboarding() {
        blockActivityCreation = true
        reopenPending = false
        onboardingSubscription?.cancel()
        onboardingSubscription = NgNavigation.openForResult(parentActivity, OnboardingRoute.Intro(), OnboardingResult::class.java,
            { AndroidUtilities.runOnUIThread({ presentFragment(LoginActivity(), true) }, 0) },
            { onOnboardingDismissed() })

        Handler(Looper.getMainLooper()).postDelayed({ blockActivityCreation = false }, 2000)
    }

    private fun onOnboardingDismissed() {
        onboardingSubscription = null
        if (!isOnStack()) return
        reopenPending = true
        scheduleReopen()
    }

    private fun scheduleReopen() {
        val activity = parentActivity ?: return
        NgNavigation.afterPopupClosed(activity) {
            AndroidUtilities.runOnUIThread({ reopenIfIdle() }, REOPEN_DELAY_MS)
        }
    }

    private fun reopenIfIdle() {
        if (!reopenPending || !isOnStack() || isPaused()) return
        openOnboarding()
    }

    private fun isOnStack(): Boolean =
        !destroyed && !isFinished && getParentLayout()?.fragmentStack?.contains(this) == true

    override fun onResume() {
        super.onResume()
        if (reopenPending && isOnStack()) scheduleReopen()
    }

    override fun onFragmentDestroy() {
        destroyed = true
        reopenPending = false
        onboardingSubscription?.cancel()
        onboardingSubscription = null
        super.onFragmentDestroy()
    }

    private companion object {
        const val REOPEN_DELAY_MS = 500L
    }
}
