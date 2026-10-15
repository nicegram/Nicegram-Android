package app.nicegram

import androidx.annotation.StringRes
import com.appvillis.core_domain.entry.openrouter.VoiceMessageTranscriptionAccessEntry
import com.appvillis.core_domain.entry.openrouter.VoiceMessageTranscriptionEntry
import com.appvillis.core_ui.domain.ToastMessage
import com.appvillis.core_ui.domain.ToastText
import com.appvillis.core_ui.widgets.ToastView
import com.appvillis.core_ui.widgets.ToastViewHelper
import com.appvillis.feature_transcription.api.TranscriptionEntryPoint
import com.appvillis.feature_transcription.api.voicemessage.VoiceMessageRouting
import com.appvillis.nicegram.NicegramAssistantEntryPoint
import dagger.hilt.EntryPoints
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.DialogObject
import org.telegram.messenger.FileLoader
import org.telegram.messenger.LocaleController
import org.telegram.messenger.MessageObject
import org.telegram.messenger.MessagesStorage
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.R
import org.telegram.messenger.UserConfig
import org.telegram.tgnet.TLRPC
import org.telegram.ui.Components.TranscribeButton
import org.telegram.ui.LaunchActivity
import timber.log.Timber
import java.io.File
import kotlin.coroutines.resume

object NgVoiceMessageTranscription {

    private const val STALL_TIMEOUT_MS = 30_000L
    private const val NG_TRANSCRIPTION_ID = Long.MIN_VALUE

    private sealed interface Outcome {
        data class Done(val text: String, val substitutedModelName: String?) : Outcome
        data class Failed(@StringRes val messageRes: Int?) : Outcome
    }

    @JvmStatic
    fun canShowButton(messageObject: MessageObject?): Boolean =
        messageObject != null && messageObject.isVoice() && !DialogObject.isEncryptedDialog(messageObject.dialogId)

    @JvmStatic
    fun shouldHandle(messageObject: MessageObject?): Boolean {
        if (messageObject?.messageOwner == null) return false
        if (!canShowButton(messageObject) || messageObject.isRoundVideo() || !messageObject.isSent()) return false
        val isTelegramPremium = UserConfig.getInstance(messageObject.currentAccount).isPremium()
        return !VoiceMessageRouting.usesTelegram(isTelegramPremium, messageObject.duration)
    }

    @JvmStatic
    fun start(messageObject: MessageObject) {
        val owner = messageObject.messageOwner ?: return
        if (owner.voiceTranscriptionFinal && owner.voiceTranscription != null) {
            reopen(messageObject)
            return
        }
        if (TranscribeButton.isTranscribing(messageObject)) return
        TranscribeButton.ngBeginTranscription(messageObject)
        notifyUpdate(messageObject)
        assistantEntryPoint().appScope().launch(Dispatchers.Main) { run(messageObject) }
    }

    private suspend fun run(messageObject: MessageObject) {
        val outcome = try {
            transcribe(messageObject)
        } catch (e: CancellationException) {
            fail(messageObject)
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Voice message transcription failed")
            Outcome.Failed(R.string.Error_Default)
        }
        when (outcome) {
            is Outcome.Done -> {
                complete(messageObject, outcome.text)
                outcome.substitutedModelName?.let {
                    showToast(ToastMessage.Warning(ToastText.Raw(LocaleController.formatString(R.string.VoiceInput_ModelSubstituted, it))))
                }
            }
            is Outcome.Failed -> {
                fail(messageObject)
                outcome.messageRes?.let {
                    showToast(ToastMessage.Error(ToastText.Raw(LocaleController.getString(it))))
                }
            }
        }
    }

    private suspend fun transcribe(messageObject: MessageObject): Outcome {
        val entryPoint = transcriptionEntryPoint()
        when (entryPoint.checkVoiceMessageTranscriptionAccessUseCase()(Unit)) {
            VoiceMessageTranscriptionAccessEntry.Allowed -> Unit
            VoiceMessageTranscriptionAccessEntry.SignInFailed -> return Outcome.Failed(R.string.Error_Default)
            VoiceMessageTranscriptionAccessEntry.NoSubscription -> {
                entryPoint.voiceInputBillingNavigator().openSpeechToTextPaywall()
                return Outcome.Failed(null)
            }
        }
        val file = awaitVoiceFile(messageObject) ?: return Outcome.Failed(R.string.Error_Default)
        return when (val result = entryPoint.transcribeVoiceMessageUseCase()(file.absolutePath)) {
            is VoiceMessageTranscriptionEntry.Text -> Outcome.Done(result.text, result.substitutedModelName)
            VoiceMessageTranscriptionEntry.NotEnoughBalance -> {
                entryPoint.voiceInputBillingNavigator().openAiTokensTopUp()
                Outcome.Failed(null)
            }
            VoiceMessageTranscriptionEntry.ModelUnavailable -> Outcome.Failed(R.string.VoiceInput_TranscribeGeoBlock)
            VoiceMessageTranscriptionEntry.Error -> Outcome.Failed(R.string.Error_Default)
        }
    }

    private fun reopen(messageObject: MessageObject) {
        val owner = messageObject.messageOwner
        TranscribeButton.openVideoTranscription(messageObject)
        owner.voiceTranscriptionOpen = true
        MessagesStorage.getInstance(messageObject.currentAccount)
            .updateMessageVoiceTranscriptionOpen(messageObject.dialogId, messageObject.id, owner)
        NotificationCenter.getInstance(messageObject.currentAccount)
            .postNotificationName(NotificationCenter.voiceTranscriptionUpdate, messageObject, null, null, true, true)
    }

    private fun complete(messageObject: MessageObject, text: String) {
        val owner = messageObject.messageOwner
        TranscribeButton.openVideoTranscription(messageObject)
        owner.voiceTranscriptionOpen = true
        owner.voiceTranscriptionId = 0
        MessagesStorage.getInstance(messageObject.currentAccount)
            .updateMessageVoiceTranscriptionOpen(messageObject.dialogId, messageObject.id, owner)
        TranscribeButton.finishTranscription(messageObject, NG_TRANSCRIPTION_ID, text)
    }

    private fun fail(messageObject: MessageObject) {
        TranscribeButton.ngEndTranscription(messageObject)
        val owner = messageObject.messageOwner
        owner.voiceTranscriptionOpen = false
        MessagesStorage.getInstance(messageObject.currentAccount)
            .updateMessageVoiceTranscriptionOpen(messageObject.dialogId, messageObject.id, owner)
        NotificationCenter.getInstance(messageObject.currentAccount)
            .postNotificationName(NotificationCenter.voiceTranscriptionUpdate, messageObject, NG_TRANSCRIPTION_ID, null, false, null)
    }

    private fun notifyUpdate(messageObject: MessageObject) {
        NotificationCenter.getInstance(messageObject.currentAccount)
            .postNotificationName(NotificationCenter.voiceTranscriptionUpdate, messageObject)
    }

    private suspend fun awaitVoiceFile(messageObject: MessageObject): File? {
        existingFile(messageObject)?.let { return it }
        val document = messageObject.document ?: return null
        return suspendCancellableCoroutine { continuation ->
            val download = VoiceFileDownload(messageObject, document) { file ->
                if (continuation.isActive) continuation.resume(file)
            }
            continuation.invokeOnCancellation { AndroidUtilities.runOnUIThread { download.cancel() } }
            download.start()
        }
    }

    private fun existingFile(messageObject: MessageObject): File? =
        FileLoader.getInstance(messageObject.currentAccount).getPathToMessage(messageObject.messageOwner)
            ?.takeIf { it.exists() && it.length() > 0 }

    private fun showToast(message: ToastMessage) {
        val parent = LaunchActivity.getSafeLastFragment()?.fragmentView ?: return
        ToastViewHelper.showViewToast(ToastView.newInstance(parent.context, message), parent, true, true, AndroidUtilities.dp(24f))
    }

    private fun assistantEntryPoint(): NicegramAssistantEntryPoint =
        EntryPoints.get(ApplicationLoader.applicationContext, NicegramAssistantEntryPoint::class.java)

    private fun transcriptionEntryPoint(): TranscriptionEntryPoint =
        EntryPoints.get(ApplicationLoader.applicationContext, TranscriptionEntryPoint::class.java)

    private class VoiceFileDownload(
        private val messageObject: MessageObject,
        private val document: TLRPC.Document,
        private val onResult: (File?) -> Unit,
    ) : NotificationCenter.NotificationCenterDelegate {

        private val loader = FileLoader.getInstance(messageObject.currentAccount)
        private val center = NotificationCenter.getInstance(messageObject.currentAccount)
        private val fileName = FileLoader.getAttachFileName(document)
        private var observers: NotificationCenter.ObserversGroup? = null
        private val stall = Runnable {
            loader.cancelLoadFile(document)
            finish(null)
        }
        private var finished = false

        fun start() {
            observers = center.createObserversGroup(this)
                .add(NotificationCenter.fileLoaded)
                .add(NotificationCenter.fileLoadFailed)
                .add(NotificationCenter.fileLoadProgressChanged)
            AndroidUtilities.runOnUIThread(stall, STALL_TIMEOUT_MS)
            loader.loadFile(document, messageObject, FileLoader.PRIORITY_HIGH, 0)
        }

        fun cancel() {
            finish(null)
        }

        override fun didReceivedNotification(id: Int, account: Int, vararg args: Any?) {
            if (args.firstOrNull() != fileName) return
            when (id) {
                NotificationCenter.fileLoaded ->
                    finish((args.getOrNull(1) as? File) ?: loader.getPathToMessage(messageObject.messageOwner))
                NotificationCenter.fileLoadFailed -> finish(null)
                NotificationCenter.fileLoadProgressChanged -> {
                    AndroidUtilities.cancelRunOnUIThread(stall)
                    AndroidUtilities.runOnUIThread(stall, STALL_TIMEOUT_MS)
                }
            }
        }

        private fun finish(file: File?) {
            if (finished) return
            finished = true
            AndroidUtilities.cancelRunOnUIThread(stall)
            observers?.removeAllObservers()
            observers = null
            onResult(file?.takeIf { it.exists() && it.length() > 0 })
        }
    }
}
