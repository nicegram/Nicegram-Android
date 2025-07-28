package app.nicegram.domain.usecases

import androidx.annotation.Keep
import app.nicegram.NicegramGroupCollectHelper
import app.nicegram.domain.entitie.ChatIdWithMessageId
import app.nicegram.groupAndLimitMessageObjects
import app.nicegram.groupAndLimitMessages
import app.nicegram.toMessageInformation
import app.nicegram.toModel
import com.appvillis.core_network.data.body.ChannelInfoRequest
import com.appvillis.feature_nicegram_client.domain.NgClientRemoteConfigRepo
import com.appvillis.lib_android_base.domain.OperationResult
import org.telegram.messenger.MessageObject
import org.telegram.tgnet.TLRPC
import timber.log.Timber
import javax.inject.Inject

@Keep
class PrepareMessagesUseCase @Inject constructor(
    private val mediaPreloaderUseCase: MediaPreloaderUseCase,
    private val uploadInitiateUseCase: UploadInitiateUseCase,
    private val uploadToS3UseCase: UploadToS3UseCase,
    private val ngRemoteConfigRepo: NgClientRemoteConfigRepo,
) {
    @Throws
    suspend fun prepare(chatId: Long, messages: List<MessageObject>): List<ChannelInfoRequest.MessageInformation> {
        val messagesWithFlags = messages.groupAndLimitMessageObjects(ngRemoteConfigRepo.messagesLimit)
        val needPreload = messagesWithFlags.filter { it.second }.map { it.first }
        var uploadedMap: Map<ChatIdWithMessageId, Int> = emptyMap()

        if (needPreload.isNotEmpty() && ngRemoteConfigRepo.collectMessageImages) {
            val preloadedMedia = mediaPreloaderUseCase.preloadMedia(chatId, needPreload)
            if (preloadedMedia.isEmpty()) {
                Timber.e("PrepareUseCase: Can't preload any images from tg")
            } else {

                val uploadBody = uploadInitiateUseCase.buildUploadInitiateBody(preloadedMedia)
                val uploadResponse = when (val result = uploadInitiateUseCase.initiateUpload(uploadBody)) {
                    is OperationResult.ResultSuccess -> result.result
                    else -> {
                        Timber.e("PrepareUseCase: Can't initiate upload to server")
                        null
                    }
                }

                uploadResponse?.let { response ->
                    if (preloadedMedia.size != response.size) {
                        Timber.e("PrepareUseCase: Mismatch between preloaded media and upload response")
                    } else {
                        val mediaWithUploadInfo = preloadedMedia.zip(response)
                        uploadedMap = uploadToS3UseCase.uploadAll(mediaWithUploadInfo, deleteAfterUpload = false)
                    }
                }
            }
        }

        return messagesWithFlags.mapNotNull { (message, shouldLoadPhoto) ->
            val uploadId = if (shouldLoadPhoto) uploadedMap[chatId to message.id] else null

            message.toMessageInformation(uploadId)
        }
    }

    suspend fun preloadAndUploadAllMedia(
        chatDataList: List<NicegramGroupCollectHelper.MoreChatFull.Data>,
        currentAccount: Int
    ): Map<ChatIdWithMessageId, Int> {
        val allMessagesToPreload = mutableListOf<Pair<Long, List<TLRPC.Message>>>() // Pair(chatId, messages)

        chatDataList.forEach { data ->
            val messagesWithFlags = data.messages.groupAndLimitMessages(ngRemoteConfigRepo.messagesLimit)
                ?: return@forEach
            val needPreload = messagesWithFlags.filter { it.second }.map { it.first }
            allMessagesToPreload += data.tlrpcChatFull.full_chat.id to needPreload
        }

        if (allMessagesToPreload.isEmpty() || !ngRemoteConfigRepo.collectMessageImages) return emptyMap()

        val preloadedMedia = allMessagesToPreload.flatMap { (chatId, messages) ->
            mediaPreloaderUseCase.preloadMedia(chatId, currentAccount, messages)
        }

        val uploadBody = uploadInitiateUseCase.buildUploadInitiateBody(preloadedMedia)
        val uploadResponse = when (val result = uploadInitiateUseCase.initiateUpload(uploadBody)) {
            is OperationResult.ResultSuccess -> result.result
            else -> {
                Timber.e("PrepareUseCase: Can't initiate upload to server")
                return emptyMap()
            }
        }

        if (preloadedMedia.size != uploadResponse.size) {
            Timber.e("PrepareUseCase: Mismatch between preloaded media and upload response")
            return emptyMap()
        }

        val mediaWithUploadInfo = preloadedMedia.zip(uploadResponse)

        return uploadToS3UseCase.uploadAll(mediaWithUploadInfo, deleteAfterUpload = true)
    }

    @Throws
    fun prepare(
        chatId: Long,
        messages: List<TLRPC.Message>?,
        chats: List<TLRPC.Chat>,
        users: List<TLRPC.User>,
        uploadedMap: Map<ChatIdWithMessageId, Int>
    ): List<ChannelInfoRequest.MessageInformation>? {
        val messagesWithFlags = messages.groupAndLimitMessages(ngRemoteConfigRepo.messagesLimit) ?: return null

        return messagesWithFlags.mapNotNull { (message, shouldLoadPhoto) ->
            val uploadId = if (shouldLoadPhoto) uploadedMap[chatId to message.id] else null

            message.toModel(chats, users, uploadId)
        }
    }
}
