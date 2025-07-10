package app.nicegram.domain.usecases

import androidx.annotation.Keep
import app.nicegram.domain.entitie.PreloadedMedia
import com.appvillis.core_network.data.body.UploadInitiateMultipleBody
import com.appvillis.feature_nicegram_client.NicegramClientHelper.preparedChatId
import com.appvillis.feature_nicegram_client.domain.CollectGroupNetworkService
import com.appvillis.feature_nicegram_client.domain.etities.UploadInformation
import com.appvillis.lib_android_base.domain.OperationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@Keep
class UploadInitiateUseCase @Inject constructor(
    private val groupNetworkService: CollectGroupNetworkService,
) {

    fun buildUploadInitiateBody(preloadedMedia: List<PreloadedMedia>): UploadInitiateMultipleBody {
        val uploads = preloadedMedia.map {
            UploadInitiateMultipleBody.UploadInitiateBody(
                fileName = null,  // it.file.name,
                mimeType = it.mimeType,
                size = null,  // it.file.length(),
                context = UploadInitiateMultipleBody.Context.TelegramChatMessage(
                    chatId = preparedChatId(it.chatId),
                    messageId = it.messageId
                )
            )
        }

        return UploadInitiateMultipleBody(uploads)
    }

    suspend fun initiateUpload(body: UploadInitiateMultipleBody): OperationResult<List<UploadInformation>> {
        return withContext(Dispatchers.IO) {
            try {
                val result = groupNetworkService.uploadInitiateMultiple(body)

                OperationResult.ResultSuccess(result)
            } catch (e: Exception) {
                Timber.e(e)
                OperationResult.mapThrowableToResult(e)
            }
        }
    }
}
