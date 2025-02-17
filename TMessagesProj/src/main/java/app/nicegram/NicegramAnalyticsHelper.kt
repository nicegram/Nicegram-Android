package app.nicegram

import com.appvillis.nicegram.NicegramAssistantEntryPoint
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.EntryPoints
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.ChatObject
import org.telegram.tgnet.TLRPC.Chat
import org.telegram.tgnet.TLRPC.ChatFull

object NicegramAnalyticsHelper {
    private fun entryPoint() = EntryPoints.get(ApplicationLoader.applicationContext, NicegramAssistantEntryPoint::class.java)

    fun trackChatOpen(chat: Chat?, chatInfo: ChatFull?) {
        val analyticsManager = entryPoint().analyticsManager()

        try {
            if (chat == null) return
            val role = if (chat.creator) "owner" else if (chat.admin_rights != null) "admin" else "user"
            val type =
                if (ChatObject.isChannel(chat)) "channel" else if (chat.gigagroup) "gigagroup" else if (chat.megagroup) "supergroup" else "group"
            val restricted = chat.restricted || (chat.restriction_reason != null && chat.restriction_reason.size > 0)
            val visibility = if (ChatObject.isPublic(chat)) "public" else "private"
            val participantsCount = chatInfo?.participants_count ?: chat.participants_count
            val roundedParticipantCount = if (participantsCount < 50) 50 else ((participantsCount / 1000) + 1) * 1000
            analyticsManager.logEvent(
                "group_open_by_$role", mapOf(
                    "type" to type,
                    "restricted" to restricted.toString(),
                    "visibility" to visibility,
                    "participantsCount" to roundedParticipantCount.toString()
                )
            )
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(Exception("Exception while logging analytics event", e))
        }
    }
}