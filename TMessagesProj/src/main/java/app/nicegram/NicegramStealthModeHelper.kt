package app.nicegram

import com.appvillis.feature_nicegram_billing.NicegramBillingHelper
import com.appvillis.nicegram.NicegramPrefs
import org.telegram.messenger.MessagesController

object NicegramStealthModeHelper {
    private val userHasNgPremiumSub get() = NicegramBillingHelper.userHasNgPremiumSub

    fun stealthModeEnabled(currentAccount: Int): Boolean {
        return userHasNgPremiumSub && MessagesController.getNicegramSettings(currentAccount)
            .getBoolean(NicegramPrefs.PREF_STEALTH_MODE_ENABLED, NicegramPrefs.PREF_STEALTH_MODE_ENABLED_DEFAULT)
    }
}
