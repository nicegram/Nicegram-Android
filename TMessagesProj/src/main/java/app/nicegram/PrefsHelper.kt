package app.nicegram

import com.appvillis.nicegram.NicegramPrefs
import org.telegram.messenger.MessagesController

object PrefsHelper {
    fun shouldSkipRead(currentAccount: Int): Boolean {
        return MessagesController.getNicegramSettings(currentAccount)
            .getBoolean(NicegramPrefs.PREF_SKIP_READ_HISTORY, NicegramPrefs.PREF_SKIP_READ_HISTORY_DEFAULT)
    }

    fun showProfileId(currentAccount: Int): Boolean {
        return MessagesController.getNicegramSettings(currentAccount)
            .getBoolean(NicegramPrefs.PREF_SHOW_PROFILE_ID, NicegramPrefs.PREF_SHOW_PROFILE_ID_DEFAULT)
    }

    fun showRegDate(currentAccount: Int): Boolean {
        return MessagesController.getNicegramSettings(currentAccount)
            .getBoolean(NicegramPrefs.PREF_SHOW_REG_DATE, NicegramPrefs.PREF_SHOW_REG_DATE_DEFAULT)
    }

    fun openLinksInBrowser(currentAccount: Int): Boolean {
        return MessagesController.getNicegramSettings(currentAccount)
            .getBoolean(NicegramPrefs.PREF_OPEN_LINKS_IN_BROWSER, NicegramPrefs.PREF_OPEN_LINKS_IN_BROWSER_DEFAULT)
    }
}