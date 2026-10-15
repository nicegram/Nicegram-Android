package app.nicegram

import com.appvillis.feature_transcription.api.TranscriptionEntryPoint
import dagger.hilt.EntryPoints
import org.telegram.messenger.ApplicationLoader

object VoiceTranscribeHelper {

    private fun entryPoint(): TranscriptionEntryPoint =
        EntryPoints.get(ApplicationLoader.applicationContext, TranscriptionEntryPoint::class.java)

    fun getSelectedModelName(): String? =
        entryPoint().fetchSelectedTranscriptionModelUseCase().invoke(Unit)?.name

    fun isTextCleanupEnabled(): Boolean =
        entryPoint().isNeedToCleanupTranscriptUseCase().invoke(Unit)

    fun setTextCleanupEnabled(value: Boolean) {
        entryPoint().setTextCleanupEnabledUseCase().invoke(value)
    }
}
