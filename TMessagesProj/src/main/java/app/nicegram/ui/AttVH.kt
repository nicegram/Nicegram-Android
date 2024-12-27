package app.nicegram.ui

import android.content.Context
import android.view.ViewGroup
import com.appvillis.feature_attention_economy.domain.entities.AttAd
import com.appvillis.feature_attention_economy.presentation.ui.banner.AttBannerChatView
import org.telegram.messenger.MessageObject
import org.telegram.ui.Components.RecyclerListView

class AttVH(val view: AttBannerChatView) : RecyclerListView.Holder(view) {
    companion object {
        fun createView(context: Context, parent: ViewGroup): AttBannerChatView {
            return AttBannerChatView(context)
        }
    }

    fun onBind(attAd: AttAd, chatId: Long?, textColor: Int, bgColor: Int, arrowColor: Int, arrowBg: Int) {
        view.setAd(attAd, if (chatId == 0L) null else chatId, textColor, bgColor, arrowBg, arrowColor)
    }

    class AttMessageObject(val attAd: AttAd, val chatId: Long?) : MessageObject(0, null) {
        override fun getId(): Int {
            return -1
        }

        override fun getFromChatId(): Long {
            return 0
        }

        override fun isUnread(): Boolean {
            return false
        }

        override fun getGroupIdForUse(): Long {
            return 0
        }

        override fun isOut(): Boolean {
            return false
        }

        override fun isOutOwner(): Boolean {
            return false
        }

        override fun getDialogId(): Long {
            return -1
        }
    }
}