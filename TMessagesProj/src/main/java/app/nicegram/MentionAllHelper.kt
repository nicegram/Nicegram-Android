package app.nicegram

import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import com.appvillis.nicegram.NicegramBillingHelper
import com.appvillis.nicegram.NicegramPrefs
import org.telegram.messenger.MessagesController
import org.telegram.messenger.UserObject
import org.telegram.tgnet.TLRPC
import org.telegram.ui.Components.URLSpanUserMention

object MentionAllHelper {
    private const val MAX_USER_COUNT = 20

    fun canUseMentionAll(chatUserCount: Int, currentAccount: Int): Boolean {
        return NicegramBillingHelper.userHasNgPremiumSub && MessagesController.getNicegramSettings(currentAccount)
            .getBoolean(NicegramPrefs.PREF_MENTION_ALL_ENABLED, NicegramPrefs.PREF_MENTION_ALL_ENABLED_DEFAULT) && chatUserCount <= MAX_USER_COUNT
    }

    fun buildMentionAllText(
        chatInfo: TLRPC.ChatFull,
        currentUserId: Long,
        messagesController: MessagesController
    ): CharSequence {
        val resultString = SpannableStringBuilder()
        for (participant in chatInfo.participants.participants) {
            if (participant.user_id == currentUserId) continue
            val participantUser: TLRPC.User = messagesController.getUser(participant.user_id)
            if (participantUser.username != null) {
                resultString
                    .append("@")
                    .append(participantUser.username)
                    .append(" ")
            } else {
                val name = UserObject.getFirstName(participantUser, false)
                val spannable: Spannable = SpannableString("$name ")
                spannable.setSpan(
                    URLSpanUserMention("" + participantUser.id, 3),
                    0,
                    spannable.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                resultString.append(spannable)
            }
        }

        return resultString
    }
}