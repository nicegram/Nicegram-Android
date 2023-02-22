package app.nicegram

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Base64
import app.nicegram.PrefsHelper.shareChannelInfo
import com.appvillis.nicegram.domain.CollectGroupInfoUseCase
import com.appvillis.nicegram.domain.CollectGroupInfoUseCase.*
import org.telegram.messenger.*
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLObject
import org.telegram.tgnet.TLRPC.*
import java.io.ByteArrayOutputStream

object NicegramGroupCollectHelper {
    var collectGroupInfoUseCase: CollectGroupInfoUseCase? = null

    fun tryToCollectChannelInfo(
        currentAccount: Int,
        currentChat: Chat?,
        messages: List<MessageObject>,
        messagesController: MessagesController,
        connectionsManager: ConnectionsManager,
        userConfig: UserConfig,
        chatInfo: ChatFull?,
        avatarDrawable: Drawable?,
        getTranslationTextCallback: (MessageObject) -> String?
    ) {
        val collectGroupInfoUseCase = collectGroupInfoUseCase ?: return
        if (currentChat == null) return

        if (!ChatObject.isChannel(currentChat)) {
            if (currentChat is TL_chat) {
                if (currentChat.participants_count < 1000) return
            } else return
        }

        if (!shareChannelInfo(currentAccount)) {
            return
        }
        if (!collectGroupInfoUseCase.canCollect(currentChat.id)) {
            return
        }
        var msgForLangDetect: String? = null
        for (message in messages) { // searching for message with length of 16 or more to detect channel lang
            if (!message.isOut) {
                val textToTranslate = getTranslationTextCallback(message)
                if (textToTranslate != null && textToTranslate.length >= 16) {
                    msgForLangDetect = textToTranslate
                    break
                }
            }
        }
        if (msgForLangDetect == null && !messages.isEmpty()) {
            for (message in messages) {
                if (!message.isOut) {
                    val textToTranslate = getTranslationTextCallback(message)
                    if (textToTranslate != null) {
                        msgForLangDetect = textToTranslate
                        break
                    }
                }
            }
        }
        if (msgForLangDetect != null) {
            LanguageDetector.detectLanguage(msgForLangDetect, { str: String? ->
                getInviteLinksAndCollect(
                    str ?: "--", currentChat, messagesController, connectionsManager, userConfig, chatInfo, avatarDrawable
                )
            }) { e: Exception? ->
                getInviteLinksAndCollect(
                    "--", currentChat, messagesController, connectionsManager, userConfig, chatInfo, avatarDrawable
                )
            }
        }
    }

    private fun getInviteLinksAndCollect(
        lang: String,
        currentChat: Chat,
        messagesController: MessagesController,
        connectionsManager: ConnectionsManager,
        userConfig: UserConfig,
        chatInfo: ChatFull?,
        avatarDrawable: Drawable?
    ) {
        val req = TL_messages_getExportedChatInvites()
        req.peer = messagesController.getInputPeer(-currentChat.id)
        req.limit = 50
        req.admin_id = messagesController.getInputUser(userConfig.currentUser)
        connectionsManager.sendRequest(req) { response: TLObject?, error: TL_error? ->
            AndroidUtilities.runOnUIThread {
                val invites: MutableList<InviteLink> = ArrayList()
                if (error == null) {
                    val invitesResponse = response as TL_messages_exportedChatInvites
                    for (inv in invitesResponse.invites) {
                        if (inv is TL_chatInviteExported) {
                            invites.add(
                                InviteLink(
                                    inv.date.toLong(),
                                    inv.request_needed,
                                    inv.admin_id,
                                    inv.permanent,
                                    inv.revoked,
                                    inv.link
                                )
                            )
                        }
                    }
                }
                collectChannelInfo(lang, invites, currentChat, chatInfo, avatarDrawable)
            }
        }
    }

    private fun collectChannelInfo(lang: String, invites: List<InviteLink>, currentChat: Chat, chatInfo: ChatFull?, avatarDrawable: Drawable?) {
        var geo: Geo? = null
        if (chatInfo != null && chatInfo.location is TL_channelLocation) {
            val loc = chatInfo.location as TL_channelLocation
            geo = Geo(loc.address, loc.geo_point.lat, loc.geo_point.lat)
        }
        val restrictions = mutableListOf<Restriction>()
        for (reason in currentChat.restriction_reason) {
            restrictions.add(Restriction(reason.platform, reason.text, reason.reason))
        }

        var avatarBase64: String? = null
        avatarDrawable?.let {
            val bitmap = (it as? BitmapDrawable)?.bitmap
            if (bitmap != null) {
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                avatarBase64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
            }
        }

        collectGroupInfoUseCase?.collectInfo(
            currentChat.id,
            invites,
            avatarBase64,
            restrictions,
            currentChat.verified,
            if (chatInfo != null) chatInfo.about else "",
            currentChat.has_geo,
            currentChat.title,
            currentChat.fake,
            currentChat.scam,
            currentChat.date.toLong(),
            currentChat.username,
            currentChat.gigagroup,
            lang,
            currentChat.participants_count,
            "",
            geo
        )
    }
}