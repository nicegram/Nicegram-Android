package app.nicegram.data

import app.nicegram.TgImagesHelper
import app.nicegram.domain.MediaUploader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.telegram.messenger.MessageObject
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.NativeByteBuffer
import org.telegram.tgnet.TLRPC
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.coroutines.resume

object TelegramMediaUploader : MediaUploader {

    override suspend fun ensureMediaUploaded(message: MessageObject): File? = withContext(Dispatchers.IO) {
        message.messageOwner.media ?: return@withContext null
        val file = TgImagesHelper.getFileForMessagePhoto(message, withLoad = true) ?: return@withContext null

        if (!file.exists()) {
            waitForFile(file)
        }

        file
    }

    private suspend fun waitForFile(file: File, timeoutMs: Long = 20_000, delayMs: Long = 1_000): Boolean = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        while (!file.exists() && System.currentTimeMillis() - start < timeoutMs) {
            delay(delayMs)
        }
        file.exists()
    }

    override suspend fun ensureMediaUploaded(message: TLRPC.Message, currentAccount: Int): File? =
        withContext(Dispatchers.IO) {
            val media = message.media
            if (media !is TLRPC.TL_messageMediaPhoto) return@withContext null

            val file = getFileForMessagePhoto(currentAccount, media) ?: return@withContext null

            file
        }

    private suspend fun getFileForMessagePhoto(
        currentAccount: Int,
        messageMediaPhoto: TLRPC.TL_messageMediaPhoto
    ): File? = suspendCancellableCoroutine { cont ->
        getMediaPhotoFileChunked(
            currentAccount,
            messageMediaPhoto,
            onSuccess = { byteBuffer ->
                if (byteBuffer == null) {
                    Timber.e("TempFileWriter", "ByteBuffer is null, cannot write to file")
                    cont.resume(null)
                    return@getMediaPhotoFileChunked
                }

                byteBuffer.buffer.rewind()
                if (byteBuffer.buffer.limit() == 0) {
                    Timber.e("TempFileWriter", "ByteBuffer is empty, skipping file creation")
                    cont.resume(null)
                    return@getMediaPhotoFileChunked
                }

                val file = byteBuffer?.writeToTempFile(messageMediaPhoto.photo.id.toString())
                cont.resume(file)
            },
            onError = {
                Timber.e(it)
                cont.resume(null)
            }
        )
    }

    private fun getMediaPhotoFileChunked(
        currentAccount: Int,
        messageMediaPhoto: TLRPC.TL_messageMediaPhoto,
        onSuccess: (byteBuffer: NativeByteBuffer?) -> Unit,
        onError: (error: String) -> Unit
    ) {
        val photo = messageMediaPhoto.photo
        val usedPhotoType = "x"
        val size = photo.sizes?.find { it.type == "x" }?.size ?: 0
        val chunkSize = 128 * 1024 // 128 KB

        try {
            val imLocation = TLRPC.TL_inputPhotoFileLocation().apply {
                id = photo.id
                access_hash = photo.access_hash
                file_reference = photo.file_reference
                thumb_size = usedPhotoType
            }

            val buffer = ByteArrayOutputStream()
            var currentOffset = 0

            fun requestNextChunk() {
                if (currentOffset >= size) {
                    try {
                        val byteArray = buffer.toByteArray()
                        val nativeBuffer = NativeByteBuffer(byteArray.size)
                        nativeBuffer.writeBytes(byteArray)
                        onSuccess(nativeBuffer)
                    } catch (e: Exception) {
                        onError("Error assembling buffer: ${e.message}")
                    }
                    return
                }

                val request = TLRPC.TL_upload_getFile().apply {
                    flags = photo.flags
                    precise = false
                    cdn_supported = true
                    location = imLocation
                    offset = currentOffset.toLong()
                    limit = chunkSize
                }

                try {
                    ConnectionsManager.getInstance(currentAccount)
                        .sendRequest(
                            request, { response, error ->
                                try {
                                    if (error != null) {
                                        onError("Chunk error: ${error.code}, ${error.text}")
                                        return@sendRequest
                                    }

                                    if (response is TLRPC.TL_upload_file) {
                                        try {
                                            val bytes = response.bytes
                                            if (bytes != null) {
                                                bytes.buffer.rewind()
                                                val rawBytes = ByteArray(bytes.buffer.limit())
                                                bytes.buffer.get(rawBytes)

                                                buffer.write(rawBytes)
                                                currentOffset += rawBytes.size

                                                requestNextChunk()
                                            } else {
                                                onError("Response buffer is null")
                                            }
                                        } catch (e: Exception) {
                                            onError("Exception while reading chunk: ${e.message}")
                                        }
                                    } else {
                                        onError("Unexpected response type")
                                    }
                                } catch (e: Exception) {
                                    onError("Exception in response handler: ${e.message}")
                                }
                            }, null, null, 0, photo.dc_id, ConnectionsManager.ConnectionTypeDownload, true
                        )
                } catch (e: Exception) {
                    onError("Exception sending request: ${e.message}")
                }
            }

            requestNextChunk()

        } catch (e: Exception) {
            onError("Exception initializing download: ${e.message}")
        }
    }

    private fun NativeByteBuffer.writeToTempFile(id: String): File {
        buffer.rewind()
        val rawBytes = ByteArray(buffer.limit())
        buffer.get(rawBytes)

        val tempFile = File.createTempFile("tg_photo_${id}", ".jpg")
        tempFile.outputStream().use { it.write(rawBytes) }

        return tempFile
    }
}
