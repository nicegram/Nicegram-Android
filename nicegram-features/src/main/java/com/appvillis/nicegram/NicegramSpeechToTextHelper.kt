package com.appvillis.nicegram

import android.content.res.Resources
import android.os.Build
import android.util.Base64
import android.util.Base64OutputStream
import com.appvillis.nicegram.NicegramScopes.ioScope
import com.appvillis.nicegram.NicegramScopes.uiScope
import com.appvillis.nicegram.network.NicegramNetwork
import com.appvillis.nicegram.network.request.GoogleSpeechRecognizeRequest
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*

object NicegramSpeechToTextHelper {
    fun recognizeSpeech(audioFile: File, tgLang: String, callback: (text: String?) -> Unit) {
        ioScope.launch {
            try {
                val deviceLang = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Resources.getSystem().configuration.locales.get(0).language
                } else {
                    Locale.getDefault().language
                }
                val alternatives = setOf(tgLang, "en").toMutableList()
                if (tgLang == deviceLang || "en" == deviceLang) {
                    alternatives.remove(deviceLang)
                }
                val result = NicegramNetwork.googleCloudApi.recognizeSpeech(
                    GoogleSpeechRecognizeRequest(
                        GoogleSpeechRecognizeRequest.Audio(fileToBytes(audioFile)),
                        GoogleSpeechRecognizeRequest.Config(
                            languageCode = deviceLang,
                            alternativeLanguageCodes = alternatives
                        )
                    )
                )
                val text: String? = result.results?.let { results ->
                    val stringBuilder = StringBuilder()
                    results.forEach { result ->
                        result.alternatives?.forEach { alt ->
                            alt.transcript?.let { stringBuilder.append(it) }
                        }
                    }
                    stringBuilder.toString()
                }
                if (text == null) {
                    uiScope.launch { callback("") }
                } else {
                    uiScope.launch { callback(text) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                uiScope.launch { callback(null) }
            }
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