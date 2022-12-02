package app.nicegram

import com.google.gson.Gson
import org.telegram.messenger.MessagesController

object QuickRepliesHelper {
    private const val PREF_QUICK_REPLIES = "PREF_QUICK_REPLIES"

    private val gson = Gson()

    fun saveReplies(replies: List<String>, currentAccount: Int) {
        MessagesController.getNicegramSettings(currentAccount)
            .edit()
            .putString(PREF_QUICK_REPLIES, gson.toJson(replies.toTypedArray()))
            .apply()
    }

    fun getSavedReplies(currentAccount: Int): List<String> {
        try {
            val json = MessagesController.getNicegramSettings(currentAccount)
                .getString(PREF_QUICK_REPLIES, null)

            val arr = gson.fromJson(json ?: return emptyList(), Array<String>::class.java)
            return arr.toList()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) e.printStackTrace()
            return emptyList()
        }
    }
}