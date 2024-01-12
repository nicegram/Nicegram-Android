package app.nicegram

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import com.appvillis.feature_nicegram_client.presentation.onboarding.NicegramOnboardingActivity
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.LoginActivity

class NicegramIntroActivity : BaseFragment() {
    companion object {
        private const val REQ_CODE_NICEGRAM_ONBOARDING = 200000
    }

    private var blockActivityCreation = false // to avoid double activity because createView called two times

    override fun createView(context: Context): View {
        if (!blockActivityCreation) {
            blockActivityCreation = true

            parentActivity.startActivityForResult(
                Intent(
                    parentActivity,
                    NicegramOnboardingActivity::class.java
                ), REQ_CODE_NICEGRAM_ONBOARDING
            )

            Handler(Looper.getMainLooper()).postDelayed({ blockActivityCreation = false }, 2000)
        }

        return FrameLayout(parentActivity).apply {
            setBackgroundColor(Color.BLACK)
        }
    }

    override fun onActivityResultFragment(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQ_CODE_NICEGRAM_ONBOARDING) {
            if (resultCode == Activity.RESULT_OK) {
                AndroidUtilities.runOnUIThread({
                    presentFragment(
                        LoginActivity(), true
                    )
                }, 0)
            } else {
                parentActivity.finish()
            }
        }
    }
}