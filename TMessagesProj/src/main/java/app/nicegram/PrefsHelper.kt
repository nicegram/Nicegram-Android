package app.nicegram

import android.content.Context
import android.content.SharedPreferences
import com.appvillis.feature_nicegram_client.domain.CommonRemoteConfigRepo
import com.appvillis.nicegram.NicegramPrefs
import org.telegram.messenger.MessagesController
import java.util.concurrent.TimeUnit

object PrefsHelper {
    var remoteConfigRepo: CommonRemoteConfigRepo? = null

    fun showProfileId(currentAccount: Int): Boolean {
        return MessagesController.getNicegramSettings(currentAccount)
            .getBoolean(NicegramPrefs.PREF_SHOW_PROFILE_ID, NicegramPrefs.PREF_SHOW_PROFILE_ID_DEFAULT)
    }

    fun showRegDate(currentAccount: Int): Boolean {
        return MessagesController.getNicegramSettings(currentAccount)
            .getBoolean(NicegramPrefs.PREF_SHOW_REG_DATE, NicegramPrefs.PREF_SHOW_REG_DATE_DEFAULT)
    }

    fun shareChannelInfo(currentAccount: Int): Boolean {
        return MessagesController.getNicegramSettings(currentAccount)
            .getBoolean(NicegramPrefs.PREF_SHARE_CHANNEL_INFO, NicegramPrefs.PREF_SHARE_CHANNEL_INFO_DEFAULT)
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

    fun bypassCopyProtection(currentAccount: Int): Boolean {
        val remote = remoteConfigRepo?.allowCopyProtectedContent ?: true
        return remote && MessagesController.getNicegramSettings(currentAccount)
            .getBoolean(NicegramPrefs.PREF_BYPASS_COPY_PROTECTION, NicegramPrefs.PREF_BYPASS_COPY_PROTECTION_DEFAULT)
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

    fun setShowAiChatBotDialogs(currentAccount: Int, show: Boolean) {
        MessagesController.getNicegramSettings(currentAccount)
            .edit()
            .putBoolean(NicegramPrefs.PREF_SHOW_AI_CHAT_BOT_DIALOGS, show)
            .apply()
    }

    fun getShowAiChatBotDialogs(currentAccount: Int): Boolean {
        return MessagesController.getNicegramSettings(currentAccount)
            .getBoolean(
                NicegramPrefs.PREF_SHOW_AI_CHAT_BOT_DIALOGS,
                NicegramPrefs.PREF_SHOW_AI_CHAT_BOT_DIALOGS_DEFAULT
            )
    }

    fun setShowPstDialogs(currentAccount: Int, show: Boolean) {
        MessagesController.getNicegramSettings(currentAccount)
            .edit()
            .putBoolean(NicegramPrefs.PREF_SHOW_PST_DIALOGS, show)
            .apply()
    }

    fun getShowPstDialogs(currentAccount: Int): Boolean {
        return MessagesController.getNicegramSettings(currentAccount)
            .getBoolean(NicegramPrefs.PREF_SHOW_PST_DIALOGS, NicegramPrefs.PREF_SHOW_PST_DIALOGS_DEFAULT)
    }

    fun setShowNuHubDialogs(currentAccount: Int, show: Boolean) {
        MessagesController.getNicegramSettings(currentAccount)
            .edit()
            .putBoolean(NicegramPrefs.PREF_SHOW_NU_HUB_DIALOGS, show)
            .apply()
    }

    fun getShowNuHubDialogs(currentAccount: Int): Boolean {
        return MessagesController.getNicegramSettings(currentAccount)
            .getBoolean(NicegramPrefs.PREF_SHOW_NU_HUB_DIALOGS, NicegramPrefs.PREF_SHOW_NU_HUB_DIALOGS_DEFAULT)
    }

    fun setShowAmbassadorDialogs(currentAccount: Int, show: Boolean) {
        MessagesController.getNicegramSettings(currentAccount)
            .edit()
            .putBoolean(NicegramPrefs.PREF_SHOW_AMBASSADOR_DIALOGS, show)
            .apply()
    }

    fun getShowAmbassadorDialogs(currentAccount: Int): Boolean {
        return MessagesController.getNicegramSettings(currentAccount)
            .getBoolean(NicegramPrefs.PREF_SHOW_AMBASSADOR_DIALOGS, NicegramPrefs.PREF_SHOW_AMBASSADOR_DIALOGS_DEFAULT)
    }

    fun setShowAiChatBotChat(currentAccount: Int, show: Boolean) {
        MessagesController.getNicegramSettings(currentAccount)
            .edit()
            .putBoolean(NicegramPrefs.PREF_SHOW_AI_CHAT_BOT_CHAT, show)
            .apply()
    }

    fun getShowAiChatBotChat(currentAccount: Int): Boolean {
        return MessagesController.getNicegramSettings(currentAccount)
            .getBoolean(NicegramPrefs.PREF_SHOW_AI_CHAT_BOT_CHAT, NicegramPrefs.PREF_SHOW_AI_CHAT_BOT_CHAT_DEFAULT)
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

    fun canShowAmbassadorBanner(context: Context): Boolean {
        return System.currentTimeMillis() - getNgGlobalPrefs(context).getLong(
            NicegramPrefs.PREF_AMBASSADOR_BANNER_TS,
            0L
        ) > TimeUnit.DAYS.toMillis(30)
    }

    fun canShowNuHubBanner(context: Context): Boolean {
        return System.currentTimeMillis() - getNgGlobalPrefs(context).getLong(
            NicegramPrefs.PREF_NU_HUB_BANNER_TS,
            0L
        ) > TimeUnit.DAYS.toMillis(30)
    }

    fun setAmbassadorBannerTs(context: Context, ts: Long) {
        getNgGlobalPrefs(context)
            .edit()
            .putLong(NicegramPrefs.PREF_AMBASSADOR_BANNER_TS, ts)
            .apply()
    }

    fun setNuHubBannerTs(context: Context, ts: Long) {
        getNgGlobalPrefs(context)
            .edit()
            .putLong(NicegramPrefs.PREF_NU_HUB_BANNER_TS, ts)
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

    fun alwaysShowSpeech2Text() = remoteConfigRepo?.alwaysShowSpeech2Text ?: false

    private fun getNgGlobalPrefs(context: Context) =
        context.getSharedPreferences("NG_GLOBAL_PREFS", Context.MODE_PRIVATE)
}