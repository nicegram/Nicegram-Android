package app.nicegram.domain.usecases

import androidx.annotation.Keep
import app.nicegram.domain.entitie.ChatIdWithMessageId
import app.nicegram.domain.entitie.PreloadedMedia
import com.appvillis.feature_nicegram_client.domain.etities.UploadInformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@Keep
class UploadToS3UseCase @Inject constructor() {

    suspend fun uploadAll(
        items: List<Pair<PreloadedMedia, UploadInformation>>,
        deleteAfterUpload: Boolean = true
    ): Map<ChatIdWithMessageId, Int> = coroutineScope {
        items.map { (media, info) ->
            async {
                val uploadId = upload(media, info, deleteAfterUpload)
                (media.chatId to media.messageId) to uploadId
            }
        }.awaitAll()
            .filter { it.second != null }
            .associate { it.first to it.second!! }
    }

    suspend fun upload(
        media: PreloadedMedia,
        uploadInfo: UploadInformation,
        deleteAfterUpload: Boolean = true
    ): Int? = withContext(Dispatchers.IO) {
        val uploadUrl = uploadInfo.uploadUrl
        val file = media.file

        if (uploadUrl == null) {
            Timber.w("Upload URL is null, returning uploadId=${uploadInfo.uploadId}")
            return@withContext uploadInfo.uploadId
        }

        if (!file.exists()) {
            Timber.w("File does not exist: ${file.absolutePath}")
            return@withContext null
        }

        try {
            val request = Request.Builder()
                .url(uploadUrl)
                .put(file.asRequestBody())
                .header("Content-Type", media.mimeType)
                .header("If-None-Match", "*")
                .build()

            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()

            client.newCall(request).execute().use { response ->
                return@withContext when (response.code) {
                    200 -> {
                        Timber.i("S3 upload successful: 200 OK")
                        uploadInfo.uploadId
                    }
                    412 -> {
                        Timber.w("S3 upload skipped: 412 Precondition Failed (already uploaded)")
                        uploadInfo.uploadId
                    }
                    else -> {
                        Timber.e("S3 upload failed: ${response.code} ${response.message}")
                        null
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception during S3 upload")
            null
        } finally {
            if (deleteAfterUpload) file.delete()
        }
    }
}
