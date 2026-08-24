package app.nicegram

import android.content.Context
import android.view.View
import com.appvillis.core_ui.util.HostScopedViewModelStoreOwner
import com.appvillis.core_ui.util.composeView
import com.appvillis.core_ui.util.hostScopedViewModelStoreOwner
import com.appvillis.core_ui.util.setHostViewTreeOwners
import com.appvillis.feature_voice_input.api.VoiceTranscribeScreen
import com.appvillis.feature_voice_input.api.VoiceTranscriptionModelColors
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import org.telegram.ui.ActionBar.ActionBar.ActionBarMenuOnItemClick
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.ActionBar.Theme

class VoiceTranscriptionNgFragment : BaseFragment() {

    // View-scoped store so the hosted @HiltViewModel is cleared when this fragment is closed
    // (a Telegram BaseFragment isn't a ViewModelStoreOwner, so without this the VM would be
    // Activity-scoped and never receive onCleared() until the app dies).
    private var viewModelStoreOwner: HostScopedViewModelStoreOwner? = null

    override fun createView(context: Context): View {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back)
        actionBar.setAllowOverlayTitle(true)
        actionBar.setTitle(LocaleController.getString(R.string.VoiceInput_TranscribeModel))
        actionBar.setActionBarMenuOnItemClick(object : ActionBarMenuOnItemClick() {
            override fun onItemClick(id: Int) {
                if (id == -1) finishFragment()
            }
        })

        val storeOwner = context.hostScopedViewModelStoreOwner()
        viewModelStoreOwner = storeOwner

        val composeView = composeView(context) {
            VoiceTranscribeScreen(
                colors = VoiceTranscriptionModelColors(
                    page = Theme.getColor(Theme.key_windowBackgroundGray),
                    card = Theme.getColor(Theme.key_windowBackgroundWhite),
                    text = Theme.getColor(Theme.key_windowBackgroundWhiteBlackText),
                    radioSelected = Theme.getColor(Theme.key_radioBackgroundChecked),
                    radioUnselected = Theme.getColor(Theme.key_radioBackground),
                    divider = Theme.getColor(Theme.key_divider),
                ),
            )
        }.apply {
            setHostViewTreeOwners(context, viewModelStoreOwner = storeOwner)
        }

        fragmentView = composeView
        return fragmentView
    }

    override fun onFragmentDestroy() {
        super.onFragmentDestroy()
        viewModelStoreOwner?.clear()
        viewModelStoreOwner = null
    }
}
