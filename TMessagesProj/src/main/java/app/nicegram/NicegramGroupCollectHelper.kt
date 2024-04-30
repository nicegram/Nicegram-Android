package app.nicegram

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Base64
import com.appvillis.feature_nicegram_client.NicegramClientHelper
import com.appvillis.feature_nicegram_client.domain.CollectGroupInfoUseCase
import com.appvillis.feature_nicegram_client.domain.CollectGroupInfoUseCase.*
import org.telegram.messenger.*
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLObject
import org.telegram.tgnet.TLRPC.*
import timber.log.Timber
import java.io.ByteArrayOutputStream

object NicegramGroupCollectHelper {
    var collectGroupInfoUseCase: CollectGroupInfoUseCase? = null

    fun tryToCollectChannelInfo(
        currentAccount: Int,
        currentChat: Chat?,
        currentUser: User?,
        messages: List<MessageObject>,
        messagesController: MessagesController,
        connectionsManager: ConnectionsManager,
        userConfig: UserConfig,
        chatInfo: ChatFull?,
        avatarDrawable: Drawable?,
        getTranslationTextCallback: (MessageObject) -> String?
    ) {
        val collectGroupInfoUseCase = collectGroupInfoUseCase ?: return
        if (currentChat == null) {
            if (currentUser != null) {
                //tryCollectBotInfo(currentUser, avatarDrawable, currentAccount)
            }
            return
        }

        if (!ChatObject.isChannel(currentChat)) {
            if (currentChat is TL_chat) {
                var pplCount = currentChat.participants_count
                if (pplCount == 0 && chatInfo != null) pplCount = chatInfo.participants_count
                if (pplCount < 1000) return
            } else return
        }

        if (NicegramClientHelper.preferences?.canShareChannels != true) {
            return
        }
        if (!collectGroupInfoUseCase.canCollectGroup(currentChat.id)) {
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
                    str ?: "--",
                    currentChat,
                    messagesController,
                    connectionsManager,
                    userConfig,
                    chatInfo,
                    avatarDrawable,
                    getTypeKey(currentChat, currentAccount)
                )
            }) { e: Exception? ->
                getInviteLinksAndCollect(
                    "--",
                    currentChat,
                    messagesController,
                    connectionsManager,
                    userConfig,
                    chatInfo,
                    avatarDrawable,
                    getTypeKey(currentChat, currentAccount)
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
        avatarDrawable: Drawable?,
        type: String,
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
                try {
                    collectChannelInfo(lang, invites, currentChat, chatInfo, avatarDrawable, type)
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
        }
    }

    private fun collectChannelInfo(
        lang: String,
        invites: List<InviteLink>,
        currentChat: Chat,
        chatInfo: ChatFull?,
        avatarDrawable: Drawable?,
        type: String,
    ) {
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

        var pplCount = currentChat.participants_count
        if (pplCount == 0 && chatInfo != null) pplCount = chatInfo.participants_count

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
            pplCount,
            "",
            geo,
            type = type,
        )
    }

    private fun tryCollectBotInfo(user: User, avatarDrawable: Drawable?, currentAccount: Int) {
        val collectGroupInfoUseCase = collectGroupInfoUseCase ?: return
        if (!user.bot) return

        if (NicegramClientHelper.preferences?.canShareBots != true) {
            return
        }
        if (!collectGroupInfoUseCase.canCollectBot(user.id)) {
            return
        }
    }

    private fun getTypeKey(currentChat: Chat, currentAccount: Int): String {

        return try {
            val isChannel = ChatObject.isChannel(currentChat.id, currentAccount)
            val isMegagroup = isChannel && currentChat.megagroup

            if (isMegagroup) "group" else "channel"
        } catch (e: Exception) {
            "channel"
        }
    }
}
