package app.nicegram

import com.appvillis.feature_analytics.domain.AnalyticsManager
import com.google.android.exoplayer2.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.telegram.messenger.ChatObject
import org.telegram.tgnet.TLObject
import org.telegram.tgnet.TLRPC.*
import java.lang.Exception

object NicegramAnalyticsHelper {
    var analyticsManager: AnalyticsManager? = null

    fun trackChatOpen(chat: Chat?, chatInfo: ChatFull?) {
        try {
            if (chat == null) return
            val role = if (chat.creator) "owner" else if (chat.admin_rights != null) "admin" else "user"
            val type = if (ChatObject.isChannel(chat)) "channel" else if (chat.gigagroup) "gigagroup" else if (chat.megagroup) "supergroup" else "group"
            val restricted = chat.restricted || (chat.restriction_reason != null && chat.restriction_reason.size > 0)
            val visibility = if (ChatObject.isPublic(chat)) "public" else "private"
            val participantsCount = chatInfo?.participants_count ?: chat.participants_count
            val roundedParticipantCount = if (participantsCount < 50) 50 else ((participantsCount / 1000) + 1) * 1000
            analyticsManager?.logEvent("group_open_by_$role", mapOf(
                "type" to type,
                "restricted" to restricted.toString(),
                "visibility" to visibility,
                "participantsCount" to roundedParticipantCount.toString()
            ))
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(Exception("Exception while logging analytics event", e))
        }
    }
}