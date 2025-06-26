package app.nicegram

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Base64
import co.touchlab.stately.concurrency.AtomicBoolean
import com.appvillis.core_network.data.body.ChannelInfoRequest
import com.appvillis.feature_nicegram_client.NicegramClientHelper
import com.appvillis.feature_nicegram_client.domain.CollectGroupInfoUseCase
import com.appvillis.feature_nicegram_client.domain.CollectGroupInfoUseCase.Geo
import com.appvillis.feature_nicegram_client.domain.CollectGroupInfoUseCase.GroupCollectInfoData
import com.appvillis.feature_nicegram_client.domain.CollectGroupInfoUseCase.InviteLink
import com.appvillis.feature_nicegram_client.domain.CollectGroupInfoUseCase.Restriction
import com.appvillis.nicegram.NicegramAssistantEntryPoint
import dagger.hilt.EntryPoints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.ApplicationLoader
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
import org.telegram.tgnet.TLRPC.Message
import org.telegram.tgnet.TLRPC.TL_channel
import org.telegram.tgnet.TLRPC.TL_channelLocation
import org.telegram.tgnet.TLRPC.TL_chat
import org.telegram.tgnet.TLRPC.TL_chatInviteExported
import org.telegram.tgnet.TLRPC.TL_chatPhoto
import org.telegram.tgnet.TLRPC.TL_error
import org.telegram.tgnet.TLRPC.TL_messages_exportedChatInvites
import org.telegram.tgnet.TLRPC.TL_messages_getExportedChatInvites
import org.telegram.tgnet.TLRPC.User
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlin.random.Random

object NicegramGroupCollectHelper {
    private fun entryPoint() =
        EntryPoints.get(ApplicationLoader.applicationContext, NicegramAssistantEntryPoint::class.java)

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
        val collectGroupInfoUseCase = entryPoint().collectGroupInfoUseCase()
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
        messagesController.getChannelRecommendations(-currentChat.id)
        var msgForLangDetect: String? = null
        val filteredMessages =
            messages.filter { it.messageOwner != null && it.messageOwner.id != 0 && it.messageOwner.from_id != null } // filtered system and Ad's messages
        for (message in filteredMessages) { // searching for message with length of 16 or more to detect channel lang
            if (!message.isOut) {
                val textToTranslate = getTranslationTextCallback(message)
                if (textToTranslate != null && textToTranslate.length >= 16) {
                    msgForLangDetect = textToTranslate
                    break
                }
            }
        }
        if (msgForLangDetect == null && filteredMessages.isNotEmpty()) {
            for (message in filteredMessages) {
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
                    getTypeKey(currentChat),
                    filteredMessages,
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
                    getTypeKey(currentChat),
                    filteredMessages,
                )
            }
        }
    }

    private sealed class MoreChatFull {
        class Data(
            val tlrpcChatFull: TLRPC.TL_messages_chatFull,
            val lang: String?,
            val type: String,
            val avatarBase64: String?,
            val similarChannels: List<Chat>,
            val messages: List<Message>?,
        ) : MoreChatFull()

        class Error(
            val username: String,
            val error: String,
        ) : MoreChatFull()
    }

    private var collectInProgress = AtomicBoolean(false)

    fun tryToCollectGroupPack(currentAccount: Int) {
        if (NicegramClientHelper.preferences?.canShareChannels != true) return

        val collectGroupInfoUseCase = entryPoint().collectGroupInfoUseCase()
        if (!collectGroupInfoUseCase.canCollectGroupPack()) return

        if (collectInProgress.value) return
        collectInProgress.value = true

        entryPoint().appScope().launch(Dispatchers.IO) {
            try {
                val usernamesForParsing = collectGroupInfoUseCase.getGroupsUsernameForCollect()
                val usernames = usernamesForParsing.usernames
                val channelInfoList = collectChannelsInfo(currentAccount, usernames)

                val groupInfo = channelInfoList.map { info ->
                    when (info) {
                        is MoreChatFull.Data -> {
                            info.tlrpcChatFull.mapToInfo(
                                lang = info.lang,
                                type = info.type,
                                avatarBase64 = info.avatarBase64,
                                channelRecommendations = info.similarChannels,
                                messages = info.messages,
                            )
                        }

                        is MoreChatFull.Error -> {
                            CollectGroupInfoUseCase.GroupCollectInfoData.CollectInfoErrorData(
                                username = info.username,
                                error = info.error,
                            )
                        }
                    }

                }
                collectGroupInfoUseCase.collectGroupsInfo(groupInfo, usernamesForParsing.requestId)
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

        getMessagesHistory(currentAccount, firstChat, onSuccess = { lastMsgLanguage, messages ->
            handleChannelRecommendations(
                currentAccount,
                firstChat,
                chatFull,
                lastMsgLanguage,
                type,
                channelInfoList,
                requestSet,
                username,
                messages = messages,
            )
        }, onError = { error, messages ->
            error.logError()
            handleChannelRecommendations(
                currentAccount,
                firstChat,
                chatFull,
                null,
                type,
                channelInfoList,
                requestSet,
                username,
                messages = messages,
            )
        })
    }

    private fun handleChannelRecommendations(
        currentAccount: Int,
        chat: TLRPC.Chat,
        chatFull: TLRPC.TL_messages_chatFull,
        lang: String?,
        type: String,
        channelInfoList: MutableList<MoreChatFull>,
        requestSet: MutableSet<String>,
        username: String,
        messages: List<TLRPC.Message>?,
    ) {
        getSimilarChannels(currentAccount, chat, onSuccess = { channelRecommendations ->
            addChannelInfoWithAvatar(
                currentAccount,
                chat,
                chatFull,
                lang,
                type,
                channelInfoList,
                requestSet,
                username,
                channelRecommendations,
                messages = messages,
            )
        }, onError = { error ->
            error.logError()
            addChannelInfoWithAvatar(
                currentAccount,
                chat,
                chatFull,
                lang,
                type,
                channelInfoList,
                requestSet,
                username,
                channelRecommendations = emptyList(),
                messages = messages,
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
        username: String,
        channelRecommendations: List<Chat>,
        messages: List<Message>?,
    ) {
        getAvatarFile(currentAccount, chat, onSuccess = { avatarBase64 ->
            channelInfoList.add(
                MoreChatFull.Data(
                    chatFull,
                    lang = lang,
                    type = type,
                    avatarBase64 = avatarBase64,
                    similarChannels = channelRecommendations,
                    messages = messages
                )
            )
            requestSet.remove(username)
        }, onError = { error ->
            error.logError()
            channelInfoList.add(
                MoreChatFull.Data(
                    chatFull,
                    lang = lang,
                    type = type,
                    avatarBase64 = null,
                    similarChannels = channelRecommendations,
                    messages = messages
                )
            )
            requestSet.remove(username)
        })
    }

    private fun getMessagesHistory(
        currentAccount: Int,
        chat: TLRPC.Chat,
        onSuccess: (lastMsgLanguage: String?, messages: List<Message>) -> Unit,
        onError: (Error, messages: List<Message>?) -> Unit
    ) {
        val messagesReq = TLRPC.TL_messages_getHistory().apply {
            peer = TLRPC.TL_inputPeerChannel().apply {
                channel_id = chat.id
                access_hash = chat.access_hash
            }
            limit = 50      // Request 50, BUT then take only the 10 most relevant ones (taking groupId into account).
        }

        ConnectionsManager.getInstance(currentAccount)
            .sendRequest(messagesReq) { response: TLObject?, error: TL_error? ->
                if (error != null) {
                    onError(Error.ServerError(error.code, error.text), null)
                } else if (response is TLRPC.messages_Messages) {
                    val messages = response.messages
                    val messageForTranslate = getSuitableTextForTranslate(messages)
                    LanguageDetector.detectLanguage(messageForTranslate,
                        { str ->
                            onSuccess(str, messages)
                        }, {
                            onError(Error.InternalError("Error while detect language"), messages)
                        })

                } else onError(Error.InternalError("Error getting messages history"), null)
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
                limit = 256 * 256
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

    private fun getSimilarChannels(
        currentAccount: Int,
        chat: TLRPC.Chat,
        onSuccess: (similarChannels: List<Chat>) -> Unit,
        onError: (Error) -> Unit
    ) {
        try {
            val channelRecomReq = TLRPC.TL_channels_getChannelRecommendations().apply {
                channel = TLRPC.TL_inputChannel().apply {
                    channel_id = chat.id
                    access_hash = chat.access_hash
                }
            }
            ConnectionsManager.getInstance(currentAccount)
                .sendRequest(channelRecomReq) { response: TLObject?, error: TL_error? ->
                    if (error != null) {
                        onError(Error.ServerError(error.code, error.text))
                    } else if (response is TLRPC.messages_Chats) {
                        val chats = response.chats
                        onSuccess(chats)
                    } else onError(Error.InternalError("Error getting channel recommendation"))
                }
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
        messages: List<MessageObject>,
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
                val similarChannels: List<Chat> = try {
                    messagesController.getChannelRecommendations(-currentChat.id).chats as List<Chat>
                } catch (e: Exception) {
                    Timber.e(e)
                    emptyList()
                }
                try {
                    collectChannelInfo(
                        lang,
                        invites,
                        currentChat,
                        chatInfo,
                        avatarDrawable,
                        type,
                        similarChannels,
                        messages
                    )
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
        similarChannels: List<Chat>,
        messages: List<MessageObject>,
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

        entryPoint().collectGroupInfoUseCase().collectInfo(
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
                usernames = currentChat.getActiveUsernames(),
                currentChat.gigagroup,
                lang,
                pplCount,
                geo,
                type = type,
                similarChannels = similarChannels.mapToSimilarInfoRequestData(),
                messages = messages.mapToMessageInformation(),
                chatPhoto = currentChat.photo.toChatPhoto(),
            )
        )
    }

    private fun tryCollectBotInfo(user: User, avatarDrawable: Drawable?, currentAccount: Int) {
        val collectGroupInfoUseCase = entryPoint().collectGroupInfoUseCase()
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
        channelRecommendations: List<Chat>,
        messages: List<Message>?,
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
            usernames = firstChat.getActiveUsernames(),
            gigagroup = firstChat.gigagroup,
            lastMessageLang = lang,
            participantsCount = pplCount,
            geoLocation = getGeo(chatFull),
            type = type,
            similarChannels = channelRecommendations.mapToSimilarInfoRequestData(),
            messages = messages?.mapToMessageInformation(chats, users),
            chatPhoto = firstChat.photo.toChatPhoto(),
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

    private fun TLRPC.Chat.getActiveUsernames(): List<String> {
        return this.usernames.filter { it.active }.map { it.username }
    }

    private fun TLRPC.ChatPhoto.toChatPhoto(): ChannelInfoRequest.ChatPhoto {
        return when (this) {
            is TL_chatPhoto -> ChannelInfoRequest.ChatPhoto.Photo(has_video, stripped_thumb, photo_id, dc_id)
            else -> ChannelInfoRequest.ChatPhoto.PhotoEmpty
        }
    }

    private fun List<Chat>.mapToSimilarInfoRequestData(): List<GroupCollectInfoData.CollectInfoData> {
        return this.mapNotNull { chat ->
            try {
                GroupCollectInfoData.CollectInfoData(
                    groupId = chat.id,
                    inviteLinks = emptyList(),
                    iconBase64 = null,
                    restrictions = chat.restriction_reason.map { it.mapToData() },
                    verified = chat.verified,
                    about = null,
                    hasGeo = chat.has_geo,
                    title = chat.title,
                    fake = chat.fake,
                    scam = chat.scam,
                    date = chat.date.toLong(),
                    username = chat.username,
                    usernames = chat.getActiveUsernames(),
                    gigagroup = chat.gigagroup,
                    lastMessageLang = null,
                    participantsCount = chat.participants_count,
                    geoLocation = null,
                    type = getTypeKey(chat),
                    chatPhoto = chat.photo.toChatPhoto(),
                )
            } catch (e: Exception) {
                Timber.e(e)
                null
            }
        }
    }

}
