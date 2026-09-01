package app.nicegram

import com.appvillis.feature_voice_input.api.VoiceInputEntryPoint
import dagger.hilt.EntryPoints
import org.telegram.messenger.ApplicationLoader

object VoiceTranscribeHelper {

    private fun entryPoint(): VoiceInputEntryPoint =
        EntryPoints.get(ApplicationLoader.applicationContext, VoiceInputEntryPoint::class.java)

    fun getSelectedModelName(): String? =
        entryPoint().fetchSelectedTranscriptionModelUseCase().invoke(Unit)?.name

    fun isTextCleanupEnabled(): Boolean =
        entryPoint().isNeedToCleanupTranscriptUseCase().invoke(Unit)

    fun setTextCleanupEnabled(value: Boolean) {
        entryPoint().setTextCleanupEnabledUseCase().invoke(value)
    }
}
