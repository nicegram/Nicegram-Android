package app.nicegram.domain.usecases

import androidx.annotation.Keep
import app.nicegram.data.TelegramMediaUploader
import app.nicegram.domain.entitie.PreloadedMedia
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.telegram.messenger.MessageObject
import org.telegram.tgnet.TLRPC
import javax.inject.Inject

@Keep
class MediaPreloaderUseCase @Inject constructor() {

    private val mediaUploader = TelegramMediaUploader

    suspend fun preloadMedia(
        chatId: Long,
        messages: List<MessageObject>,
    ): List<PreloadedMedia> = coroutineScope {
        val deferredList = messages.map { message ->
            async {
                val file = mediaUploader.ensureMediaUploaded(message)
                if (file != null && file.exists()) {
                    PreloadedMedia(
                        messageId = message.messageOwner.id.toLong(),
                        chatId = chatId,
                        file = file,
                        mimeType = "image/jpeg",
                    )
                } else null
            }
        }

        deferredList.awaitAll().filterNotNull()
    }

    suspend fun preloadMedia(
        chatId: Long,
        currentAccount: Int,
        messages: List<TLRPC.Message>,
    ): List<PreloadedMedia> = coroutineScope {
        val deferredList = messages.map { message ->
            async {
                val file = mediaUploader.ensureMediaUploaded(message, currentAccount)
                if (file != null && file.exists()) {
                    PreloadedMedia(
                        messageId = message.id.toLong(),
                        chatId = chatId,
                        file = file,
                        mimeType = "image/jpeg",
                    )
                } else null
            }
        }

        deferredList.awaitAll().filterNotNull()
    }
}
