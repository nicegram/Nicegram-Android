package app.nicegram

import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.util.Base64
import android.util.Base64OutputStream
import com.appvillis.core_network.data.body.Speech2TextBody
import com.appvillis.nicegram.NicegramBillingHelper
import com.appvillis.feature_nicegram_billing.NicegramConsts.NICEGRAM_PREMIUM_SUB_ID
import com.appvillis.nicegram.NicegramAssistantEntryPoint
import com.appvillis.nicegram.NicegramScopes.ioScope
import com.appvillis.nicegram.NicegramScopes.uiScope
import dagger.hilt.EntryPoints
import kotlinx.coroutines.launch
import org.telegram.messenger.ApplicationLoader
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*

object NicegramSpeechToTextHelper {
    private fun entryPoint() =
        EntryPoints.get(ApplicationLoader.applicationContext, NicegramAssistantEntryPoint::class.java)

    private const val PROVIDER_OPEN_AI = "OPEN_AI"
    private const val PROVIDER_GOOGLE = "GOOGLE"

    fun recognizeSpeech(context: Context, audioFile: File, tgLang: String, callback: (text: String?) -> Unit) {
        val provider = pickProvider(context)
        val deviceLang = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Resources.getSystem().configuration.locales.get(0).language
        } else {
            Locale.getDefault().language
        }
        val alternatives = setOf(tgLang, "en").toMutableList()
        if (tgLang == deviceLang || "en" == deviceLang) {
            alternatives.remove(deviceLang)
        }

        ioScope.launch {
            try {
                val result = entryPoint().apiService().speech2Text(Speech2TextBody(fileToBytes(audioFile), provider, deviceLang, alternatives, entryPoint().billingManager().currentSubPurchaseToken, NICEGRAM_PREMIUM_SUB_ID)).data?.text
                uiScope.launch { callback(result) }
            } catch (e: Exception) {
                Timber.e(e)
                uiScope.launch { callback(null) }
            }

        }
    }

    private fun pickProvider(context: Context): String {
        return if (NicegramBillingHelper.getUserHasNgPremiumSub(context) && PrefsHelper.getSpeech2TextOpenAi(context)) {
            PROVIDER_OPEN_AI
        } else {
            PROVIDER_GOOGLE
        }
    }

    private fun fileToBytes(file: File): String {
        return ByteArrayOutputStream().use { outputStream ->
            Base64OutputStream(outputStream, Base64.NO_WRAP).use { base64FilterStream ->
                file.inputStream().use { inputStream ->
                    inputStream.copyTo(base64FilterStream)
                }
            }
            return@use outputStream.toString()
        }
    }
}