package com.appvillis.nicegram.network.request

import java.util.*

class GoogleSpeechRecognizeRequest(val audio: Audio, val config: Config) {
    class Audio(val content: String)
    class Config(
        val encoding: String = "OGG-OPUS",
        val sampleRateHertz: Int = 48000,
        val languageCode: String = Locale.getDefault().language,
        val alternativeLanguageCodes: List<String> = listOf("en"),
        val enableAutomaticPunctuation: Boolean = true
    )
}