package app.nicegram

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Base64
import app.nicegram.ui.AttVH
import co.touchlab.stately.concurrency.AtomicBoolean
import com.appvillis.feature_nicegram_client.NicegramClientHelper
import com.appvillis.feature_nicegram_client.domain.CollectGroupInfoUseCase
import com.appvillis.feature_nicegram_client.domain.CollectGroupInfoUseCase.Geo
import com.appvillis.feature_nicegram_client.domain.CollectGroupInfoUseCase.InviteLink
import com.appvillis.feature_nicegram_client.domain.CollectGroupInfoUseCase.Restriction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.ChatObject
import org.telegram.messenger.LanguageDetector
import org.telegram.messenger.MessageObject
import org.telegram.messenger.MessagesController
import org.telegram.messenger.UserConfig
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.NativeByteBuffer
import org.telegram.tgnet.TLObject
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.TLRPC.Chat
import org.telegram.tgnet.TLRPC.ChatFull
import org.telegram.tgnet.TLRPC.TL_channel
import org.telegram.tgnet.TLRPC.TL_channelLocation
import org.telegram.tgnet.TLRPC.TL_chat
import org.telegram.tgnet.TLRPC.TL_chatInviteExported
import org.telegram.tgnet.TLRPC.TL_error
import org.telegram.tgnet.TLRPC.TL_messages_exportedChatInvites
import org.telegram.tgnet.TLRPC.TL_messages_getExportedChatInvites
import org.telegram.tgnet.TLRPC.User
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlin.random.Random

object NicegramGroupCollectHelper {
    var collectGroupInfoUseCase: CollectGroupInfoUseCase? = null
    var appScope: CoroutineScope? = null

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
            if (message is AttVH.AttMessageObject) continue

            if (!message.isOut) {
                val textToTranslate = getTranslationTextCallback(message)
                if (textToTranslate != null && textToTranslate.length >= 16) {
                    msgForLangDetect = textToTranslate
                    break
                }
            }
        }
        if (msgForLangDetect == null && messages.isNotEmpty()) {
            for (message in messages) {
                if (message is AttVH.AttMessageObject) continue

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
                    str,
                    currentChat,
                    messagesController,
                    connectionsManager,
                    userConfig,
                    chatInfo,
                    avatarDrawable,
                    getTypeKey(currentChat)
                )
            }) { e: Exception? ->
                getInviteLinksAndCollect(
                    null,
                    currentChat,
                    messagesController,
                    connectionsManager,
                    userConfig,
                    chatInfo,
                    avatarDrawable,
                    getTypeKey(currentChat)
                )
            }
        }
    }

    private sealed class MoreChatFull {
        class Data(
            val tlrpcChatFull: TLRPC.TL_messages_chatFull,
            val lang: String?,
            val type: String,
            val avatarBase64: String?
        ) : MoreChatFull()

        class Error(
            val username: String,
            val error: String,
        ) : MoreChatFull()
    }

    private var collectInProgress = AtomicBoolean(false)

    fun tryToCollectGroupPack(currentAccount: Int) {
        if (NicegramClientHelper.preferences?.canShareChannels != true) return

        val collectGroupInfoUseCase = collectGroupInfoUseCase ?: return
        if (!collectGroupInfoUseCase.canCollectGroupPack()) return

        if (collectInProgress.value) return
        collectInProgress.value = true

        appScope?.launch(Dispatchers.IO) {
            try {
                val usernameWithToken = collectGroupInfoUseCase.getGroupsUsernameForCollect()
                val channelInfoList = collectChannelsInfo(currentAccount, usernameWithToken.keys.toList())

                collectGroupInfoUseCase.collectGroupsInfo(
                    channelInfoList.map { info ->
                        when (info) {
                            is MoreChatFull.Data -> {
                                val token = usernameWithToken[info.tlrpcChatFull.chats.first().username]

                                info.tlrpcChatFull.mapToInfo(
                                    lang = info.lang,
                                    type = info.type,
                                    avatarBase64 = info.avatarBase64,
                                    token = token
                                )
                            }

                            is MoreChatFull.Error -> {
                                val token = usernameWithToken[info.username]

                                CollectGroupInfoUseCase.GroupCollectInfoData.CollectInfoErrorData(
                                    username = info.username,
                                    error = info.error,
                                    token = token
                                )
                            }
                        }

                    }
                )
            } catch (e: Exception) {
                Timber.e(e)
            } finally {
                collectInProgress.value = false
            }
        }
    }

    private suspend fun collectChannelsInfo(currentAccount: Int, usernames: List<String>): List<MoreChatFull> {
        val channelInfoList = mutableListOf<MoreChatFull>()
        val requestSet = mutableSetOf<String>()
        val startRequestsTime = System.currentTimeMillis()
        val throttling = TimeUnit.SECONDS.toMillis(60)

        usernames.forEach { username ->
            requestSet.add(username)
            collectInfoForUsername(currentAccount, username, channelInfoList, requestSet)

            if (username != usernames.last()) delay(Random.nextLong(1000, 2000 + 1))
        }

        do {
            delay(1000)
        } while (requestSet.isNotEmpty() && (System.currentTimeMillis() - startRequestsTime < throttling))
        return channelInfoList
    }

    private fun collectInfoForUsername(
        currentAccount: Int,
        username: String,
        channelInfoList: MutableList<MoreChatFull>,
        requestSet: MutableSet<String>
    ) {
        resolveUsername(currentAccount, username, onSuccess = { resolvedPeer ->
            getFullChannelInfo(currentAccount, resolvedPeer, onSuccess = { chatFull ->
                handleSuccessfulChannelInfo(currentAccount, username, chatFull, channelInfoList, requestSet)
            }, onError = { error ->
                error.logError()
                channelInfoList.add(MoreChatFull.Error(username, error.message))
                requestSet.remove(username)
            })
        }, onError = { error ->
            error.logError()
            channelInfoList.add(MoreChatFull.Error(username, error.message))
            requestSet.remove(username)
        })
    }

    private fun resolveUsername(
        currentAccount: Int,
        username: String,
        onSuccess: (TLRPC.TL_contacts_resolvedPeer) -> Unit,
        onError: (Error) -> Unit
    ) {
        val resolveReq = TLRPC.TL_contacts_resolveUsername().apply {
            this.username = username
        }

        ConnectionsManager.getInstance(currentAccount)
            .sendRequest(resolveReq) { response: TLObject?, error: TL_error? ->
                if (error != null) {
                    onError(Error.ServerError(error.code, error.text))
                } else if (response is TLRPC.TL_contacts_resolvedPeer) {
                    onSuccess(response)
                } else onError(Error.InternalError("No resolved data"))
            }
    }

    private fun getFullChannelInfo(
        currentAccount: Int,
        resolvedPeer: TLRPC.TL_contacts_resolvedPeer,
        onSuccess: (TLRPC.TL_messages_chatFull) -> Unit,
        onError: (Error) -> Unit
    ) {
        try {
            val firstChat = resolvedPeer.chats.first()

            val channelReq = if (firstChat is TL_channel) TLRPC.TL_channels_getFullChannel().apply {
                channel = TLRPC.TL_inputChannel().apply {
                    channel_id = firstChat.id
                    access_hash = firstChat.access_hash
                }
            } else TLRPC.TL_messages_getFullChat().apply {
                chat_id = firstChat.id
            }

            ConnectionsManager.getInstance(currentAccount)
                .sendRequest(channelReq) { response: TLObject?, error: TL_error? ->
                    if (error != null) {
                        onError(Error.ServerError(error.code, error.text))
                    } else if (response is TLRPC.TL_messages_chatFull) {
                        onSuccess(response)
                    } else onError(Error.InternalError("Error getting channel info"))
                }
        } catch (e: Exception) {
            Timber.e(e)
            onError(Error.InternalError(e.message ?: "Error getting channel info"))
        }
    }

    private fun handleSuccessfulChannelInfo(
        currentAccount: Int,
        username: String,
        chatFull: TLRPC.TL_messages_chatFull,
        channelInfoList: MutableList<MoreChatFull>,
        requestSet: MutableSet<String>
    ) {
        val firstChat = chatFull.chats.first()
        val type = getTypeKey(firstChat)

        getMessagesHistory(currentAccount, firstChat, onSuccess = { lastMsgLanguage ->
            addChannelInfoWithAvatar(
                currentAccount,
                firstChat,
                chatFull,
                lastMsgLanguage,
                type,
                channelInfoList,
                requestSet,
                username
            )
        }, onError = { error ->
            error.logError()
            addChannelInfoWithAvatar(
                currentAccount,
                firstChat,
                chatFull,
                null,
                type,
                channelInfoList,
                requestSet,
                username
            )
        })
    }

    private fun addChannelInfoWithAvatar(
        currentAccount: Int,
        chat: TLRPC.Chat,
        chatFull: TLRPC.TL_messages_chatFull,
        lang: String?,
        type: String,
        channelInfoList: MutableList<MoreChatFull>,
        requestSet: MutableSet<String>,
        username: String
    ) {
        getAvatarFile(currentAccount, chat, onSuccess = { avatarBase64 ->
            channelInfoList.add(MoreChatFull.Data(chatFull, lang = lang, type = type, avatarBase64 = avatarBase64))
            requestSet.remove(username)
        }, onError = { error ->
            error.logError()
            channelInfoList.add(MoreChatFull.Data(chatFull, lang = lang, type = type, avatarBase64 = null))
            requestSet.remove(username)
        })
    }

    private fun getMessagesHistory(
        currentAccount: Int,
        chat: TLRPC.Chat,
        onSuccess: (lastMsgLanguage: String?) -> Unit,
        onError: (Error) -> Unit
    ) {
        val messagesReq = TLRPC.TL_messages_getHistory().apply {
            peer = TLRPC.TL_inputPeerChannel().apply {
                channel_id = chat.id
                access_hash = chat.access_hash
            }
            limit = 3
        }

        ConnectionsManager.getInstance(currentAccount)
            .sendRequest(messagesReq) { response: TLObject?, error: TL_error? ->
                if (error != null) {
                    onError(Error.ServerError(error.code, error.text))
                } else if (response is TLRPC.messages_Messages) {
                    val messageForTranslate = getSuitableTextForTranslate(response.messages)
                    LanguageDetector.detectLanguage(messageForTranslate,
                        { str ->
                            onSuccess(str)
                        }, {
                            onError(Error.InternalError("Error while detect language"))
                        })

                } else onError(Error.InternalError("Error getting messages history"))
            }
    }

    private fun getAvatarFile(
        currentAccount: Int,
        chat: TLRPC.Chat,
        onSuccess: (avatarBase64: String?) -> Unit,
        onError: (Error) -> Unit
    ) {
        val photo = chat.photo
        if (photo is TLRPC.TL_chatPhotoEmpty) {
            onError(Error.InternalError("Chat photo is empty"))
            return
        }

        try {
            val imLocation = TLRPC.TL_inputPeerPhotoFileLocation().apply {
                flags = photo.flags
                big = true
                peer = TLRPC.TL_inputPeerChannel().apply {
                    channel_id = chat.id
                    access_hash = chat.access_hash
                }
                photo_id = photo.photo_id
            }
            val avatarReq = TLRPC.TL_upload_getFile().apply {
                flags = photo.flags
                precise = false
                cdn_supported = true
                location = imLocation
                offset = 0
                limit = 1024 * 1024
            }
            ConnectionsManager.getInstance(currentAccount)
                .sendRequest(avatarReq, { response: TLObject?, error: TL_error? ->
                    if (error != null) {
                        onError(Error.ServerError(error.code, error.text))
                    } else if (response is TLRPC.TL_upload_file) {
                        val avatar = response.bytes.getBase64EncodedString()
                        onSuccess(avatar)
                    } else onError(Error.InternalError("Error getting avatar file"))
                }, null, null, 0, photo.dc_id, ConnectionsManager.ConnectionTypeDownload, true)
        } catch (e: Exception) {
            onError(Error.InternalError(e.message ?: "Internal Error"))
        }
    }

    private fun getInviteLinksAndCollect(
        lang: String?,
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
                            invites.add(inv.mapToData())
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
        lang: String?,
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
            restrictions.add(reason.mapToData())
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

        val pplCount = getPplCount(currentChat, chatInfo)

        collectGroupInfoUseCase?.collectInfo(
            CollectGroupInfoUseCase.GroupCollectInfoData.CollectInfoData(
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
                geo,
                type = type,
                token = null,
            )
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

    private fun resolveId(id: Long): Long {
        if (id >= 0) {
            return id
        }

        var resultId = -id
        if (resultId > 1000000000000) {
            resultId -= 1000000000000
        }

        return resultId
    }

    private fun getTypeKey(currentChat: Chat): String {

        return try {
            val isChannel = ChatObject.isChannel(currentChat)
            val isMegagroup = isChannel && currentChat.megagroup

            if (isMegagroup) "group" else "channel"
        } catch (e: Exception) {
            "channel"
        }
    }

    private fun getPplCount(chat: Chat, chatInfo: ChatFull?): Int {
        var pplCount = chat.participants_count
        if (pplCount == 0 && chatInfo != null) pplCount = chatInfo.participants_count

        return pplCount
    }

    private fun getGeo(chatInfo: ChatFull?): Geo? {
        return if (chatInfo != null && chatInfo.location is TL_channelLocation) {
            val loc = chatInfo.location as TL_channelLocation
            Geo(loc.address, loc.geo_point.lat, loc.geo_point.lat)
        } else null
    }

    private fun TLRPC.TL_messages_chatFull.mapToInfo(
        lang: String?,
        type: String,
        avatarBase64: String?,
        token: String?,
    ): CollectGroupInfoUseCase.GroupCollectInfoData.CollectInfoData {
        val chatFull = this.full_chat
        val chats = this.chats
        val users = this.users

        val firstChat = chats.first()
        val inviteLinks: List<InviteLink> = chatFull.exported_invite?.let { listOf(it.mapToData()) } ?: emptyList()
        val pplCount = getPplCount(firstChat, chatFull)

        return CollectGroupInfoUseCase.GroupCollectInfoData.CollectInfoData(
            groupId = chatFull.id,
            inviteLinks = inviteLinks,
            iconBase64 = avatarBase64,
            restrictions = firstChat.restriction_reason.map { it.mapToData() },
            verified = firstChat.verified,
            about = chatFull.about,
            hasGeo = firstChat.has_geo,
            title = firstChat.title,
            fake = firstChat.fake,
            scam = firstChat.scam,
            date = firstChat.date.toLong(),
            username = firstChat.username,
            gigagroup = firstChat.gigagroup,
            lastMessageLang = lang,
            participantsCount = pplCount,
            geoLocation = getGeo(chatFull),
            type = type,
            token = token,
        )
    }

    private fun TLRPC.TL_chatInviteExported.mapToData() = InviteLink(
        date.toLong(),
        request_needed,
        admin_id,
        permanent,
        revoked,
        link
    )

    private fun TLRPC.RestrictionReason.mapToData() = Restriction(platform, text, reason)

    private fun NativeByteBuffer.getBase64EncodedString(): String? {
        this.buffer.rewind()
        val rawBytes = ByteArray(this.buffer.limit())
        this.buffer.get(rawBytes)
        return Base64.encodeToString(rawBytes, Base64.NO_WRAP)
    }

    private sealed class Error(val message: String) {
        data class ServerError(val code: Int, val text: String) : Error(message = text)
        data class InternalError(val text: String) : Error(message = text)
    }

    private fun Error.logError() {
        when (this) {
            is Error.InternalError -> Timber.e(this.text)
            is Error.ServerError -> Timber.e("${this.code}: ${this.text}")
        }
    }

    private fun getSuitableTextForTranslate(message: List<TLRPC.Message>): String {
        val messagesText: List<String> = message.map { it.message }
        return try {
            messagesText.first { message -> message.length > 16 }
        } catch (e: Exception) {
            messagesText.random()
        }
    }
}
