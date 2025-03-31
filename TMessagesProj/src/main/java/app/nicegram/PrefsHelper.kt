package app.nicegram

import android.content.Context
import android.content.SharedPreferences
import com.appvillis.core_ui.BuildConfig
import com.appvillis.nicegram.AnalyticsHelper.logEvent
import com.appvillis.nicegram.NicegramAssistantEntryPoint
import com.appvillis.nicegram.NicegramPrefs
import com.appvillis.nicegram.NicegramPrefs.PREF_FOREVER_COOL_DOWN
import dagger.hilt.EntryPoints
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.MessagesController
import java.util.concurrent.TimeUnit

object PrefsHelper {
    private fun entryPoint(context: Context) =
        EntryPoints.get(context.applicationContext, NicegramAssistantEntryPoint::class.java)

    fun showProfileId(currentAccount: Int): Boolean {
        return MessagesController.getNicegramSettings(currentAccount)
            .getBoolean(NicegramPrefs.PREF_SHOW_PROFILE_ID, NicegramPrefs.PREF_SHOW_PROFILE_ID_DEFAULT)
    }

    fun showRegDate(currentAccount: Int): Boolean {
        return MessagesController.getNicegramSettings(currentAccount)
            .getBoolean(NicegramPrefs.PREF_SHOW_REG_DATE, NicegramPrefs.PREF_SHOW_REG_DATE_DEFAULT)
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
            .getBoolean(
                NicegramPrefs.PREF_DOWNLOAD_VIDEOS_TO_GALLERY,
                NicegramPrefs.PREF_DOWNLOAD_VIDEOS_TO_GALLERY_DEFAULT
            )
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
            .getString(
                NicegramPrefs.PREF_CHAT_LANGUAGE_TO_TRANSLATE + currentChat,
                NicegramPrefs.PREF_CHAT_LANGUAGE_TO_TRANSLATE_DEFAULT
            ) ?: NicegramPrefs.PREF_CHAT_LANGUAGE_TO_TRANSLATE_DEFAULT
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

    fun bypassCopyProtection(context: Context): Boolean {
        return entryPoint(context).ngClientRemoteConfigRepo().allowCopyProtectedContent
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

    fun getMaxAccountCountWasSet(context: Context): Boolean {
        return getNgGlobalPrefs(context)
            .getBoolean(NicegramPrefs.PREF_MAX_ACCOUNTS_SET, NicegramPrefs.PREF_MAX_ACCOUNTS_SET_DEFAULT)
    }

    fun setMaxAccountCountWasSet(context: Context) {
        getNgGlobalPrefs(context)
            .edit()
            .putBoolean(NicegramPrefs.PREF_MAX_ACCOUNTS_SET, true)
            .apply()
    }

    fun getMaxAccountCount(context: Context): Int {
        return getNgGlobalPrefs(context)
            .getInt(NicegramPrefs.PREF_MAX_ACCOUNTS, NicegramPrefs.PREF_MAX_ACCOUNTS_DEFAULT)
    }

    fun setMaxAccountCount(context: Context, count: Int) {
        getNgGlobalPrefs(context)
            .edit()
            .putInt(NicegramPrefs.PREF_MAX_ACCOUNTS, count)
            .apply()
    }

    fun canShowChatBannerWithId(context: Context, bannerId: String): Boolean {
        val showTime = getNgGlobalPrefs(context).getLong("${NicegramPrefs.PREF_CHAT_BANNER_TS_WITH_ID_}$bannerId", 0)
        // Check for the "forever coolDown" special value
        if (showTime == PREF_FOREVER_COOL_DOWN) {
            return false // If "forever coolDown" is set, the banner should not be shown again
        }
        return System.currentTimeMillis() >= showTime
    }

    fun setCdForChatBannerWithId(context: Context, coolDownSec: Int, bannerId: String) {
        val targetTime = if (coolDownSec == -1) {
            PREF_FOREVER_COOL_DOWN // Special value "forever coolDown"
        } else {
            System.currentTimeMillis().plus(TimeUnit.SECONDS.toMillis(coolDownSec.toLong()))
        }

        getNgGlobalPrefs(context)
            .edit()
            .putLong("${NicegramPrefs.PREF_CHAT_BANNER_TS_WITH_ID_}$bannerId", targetTime)
            .apply()
    }

    fun getSpeech2TextOpenAi(context: Context): Boolean {
        return getNgGlobalPrefs(context)
            .getBoolean(NicegramPrefs.PREF_S2TEXT_OPEN_AI_ENABLED, NicegramPrefs.PREF_S2TEXT_OPEN_AI_ENABLED_DEFAULT)
    }

    fun setSpeech2TextOpenAi(context: Context, enabled: Boolean) {
        getNgGlobalPrefs(context)
            .edit()
            .putBoolean(NicegramPrefs.PREF_S2TEXT_OPEN_AI_ENABLED, enabled)
            .apply()
    }

    fun setSpeech2TextBulletinSeen(context: Context) {
        getNgGlobalPrefs(context)
            .edit()
            .putBoolean(NicegramPrefs.PREF_S2TEXT_BULLET_SEEN, true)
            .apply()
    }

    fun getSpeech2TextBulletinSeen(context: Context): Boolean {
        return getNgGlobalPrefs(context)
            .getBoolean(NicegramPrefs.PREF_S2TEXT_BULLET_SEEN, NicegramPrefs.PREF_S2TEXT_BULLET_SEEN_DEFAULT)
    }

    fun alwaysShowSpeech2Text(context: Context) = if (BuildConfig.IS_LITE_CLIENT) false else entryPoint(context).ngClientRemoteConfigRepo().alwaysShowSpeech2Text

    fun setShowNgFloatingMenuInChat(currentAccount: Int, show: Boolean) {
        MessagesController.getNicegramSettings(currentAccount)
            .edit()
            .putBoolean(NicegramPrefs.PREF_SHOW_FLOATING_NG_MENU_IN_CHAT, show)
            .apply()
    }

    fun getShowNgFloatingMenuInChat(currentAccount: Int): Boolean {
        return MessagesController.getNicegramSettings(currentAccount)
            .getBoolean(
                NicegramPrefs.PREF_SHOW_FLOATING_NG_MENU_IN_CHAT,
                NicegramPrefs.PREF_SHOW_FLOATING_NG_MENU_IN_CHAT_DEFAULT
            )
    }

    fun setShowFoldersForKeywords(currentAccount: Int, show: Boolean) {
        if (!show) logEvent(ApplicationLoader.applicationContext, "keywords_folder_disabled", null)

        MessagesController.getNicegramSettings(currentAccount)
            .edit()
            .putBoolean(NicegramPrefs.PREF_SHOW_FOLDERS_FOR_KEYWORDS, show)
            .apply()
    }

    fun getShowFoldersForKeywords(currentAccount: Int): Boolean {
        return MessagesController.getNicegramSettings(currentAccount)
            .getBoolean(
                NicegramPrefs.PREF_SHOW_FOLDERS_FOR_KEYWORDS,
                NicegramPrefs.PREF_SHOW_FOLDERS_FOR_KEYWORDS_DEFAULT
            )
    }

    private fun getNgGlobalPrefs(context: Context) =
        context.getSharedPreferences("NG_GLOBAL_PREFS", Context.MODE_PRIVATE)
}
