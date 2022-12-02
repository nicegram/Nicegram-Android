package app.nicegram

import android.content.SharedPreferences
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

    fun showQuickTranslateButton(currentAccount: Int): Boolean {
        return MessagesController.getNicegramSettings(currentAccount)
            .getBoolean(NicegramPrefs.PREF_QUICK_TRANSLATE, NicegramPrefs.PREF_QUICK_TRANSLATE_DEFAULT)
    }

    fun enableStartWithRearCamera(currentAccount: Int): Boolean {
        return MessagesController.getNicegramSettings(currentAccount)
            .getBoolean(NicegramPrefs.PREF_START_WITH_REAR_CAMERA, NicegramPrefs.PREF_START_WITH_REAR_CAMERA_DEFAULT)
    }

    fun shouldDownloadVideosToGallery(currentAccount: Int): Boolean {
        return MessagesController.getNicegramSettings(currentAccount)
            .getBoolean(NicegramPrefs.PREF_DOWNLOAD_VIDEOS_TO_GALLERY, NicegramPrefs.PREF_DOWNLOAD_VIDEOS_TO_GALLERY_DEFAULT)
    }

    fun hidePhoneNumber(currentAccount: Int): Boolean {
        return MessagesController.getNicegramSettings(currentAccount)
            .getBoolean(NicegramPrefs.PREF_HIDE_PHONE_NUMBER, NicegramPrefs.PREF_HIDE_PHONE_NUMBER_DEFAULT)
    }

    fun hideReactions(currentAccount: Int): Boolean {
        return MessagesController.getNicegramSettings(currentAccount)
            .getBoolean(NicegramPrefs.PREF_HIDE_REACTIONS, NicegramPrefs.PREF_HIDE_REACTIONS_DEFAULT)
    }
    // region ng translate input text
    fun getTranslateLanguageToShortName(currentAccount: Int, currentChat: Long): String {
        return MessagesController.getNicegramSettings(currentAccount)
            .getString(NicegramPrefs.PREF_CHAT_LANGUAGE_TO_TRANSLATE + currentChat, NicegramPrefs.PREF_CHAT_LANGUAGE_TO_TRANSLATE_DEFAULT) ?: NicegramPrefs.PREF_CHAT_LANGUAGE_TO_TRANSLATE_DEFAULT
    }

    fun setTranslateLanguageToShortName(currentAccount: Int, currentChat: Long, newLocaleShortName: String) {
        val preferences: SharedPreferences = MessagesController.getNicegramSettings(currentAccount)
        val editor: SharedPreferences.Editor = preferences.edit()
        editor.putString(NicegramPrefs.PREF_CHAT_LANGUAGE_TO_TRANSLATE + currentChat, newLocaleShortName)
        editor.apply()
    }

    fun isCurrentLanguageTheDefault(currentAccount: Int, currentChat: Long): Boolean {
        return getTranslateLanguageToShortName(
            currentAccount, currentChat
        ) == NicegramPrefs.PREF_CHAT_LANGUAGE_TO_TRANSLATE_DEFAULT
    }
    // endregion ng translate input text

    fun saveFolderOnExit(currentAccount: Int): Boolean {
        return MessagesController.getNicegramSettings(currentAccount)
            .getBoolean(NicegramPrefs.PREF_SAVE_FOLDER_ON_EXIT, NicegramPrefs.PREF_SAVE_FOLDER_ON_EXIT_DEFAULT)
    }

    fun setCurrentFolder(currentAccount: Int, folder: Int) {
        MessagesController.getNicegramSettings(currentAccount)
            .edit()
            .putInt(NicegramPrefs.PREF_SAVED_FOLDER, folder)
            .apply()
    }

    fun getCurrentFolder(currentAccount: Int): Int {
        return MessagesController.getNicegramSettings(currentAccount)
            .getInt(NicegramPrefs.PREF_SAVED_FOLDER, NicegramPrefs.PREF_SAVED_FOLDER_DEFAULT)
    }
}