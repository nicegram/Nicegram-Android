package com.appvillis.nicegram

import com.appvillis.nicegram.NicegramNetworkConsts.TRANSLATE_URL
import com.appvillis.nicegram.NicegramScopes.ioScope
import com.appvillis.nicegram.NicegramScopes.uiScope
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import java.net.URLEncoder

object NicegramTranslator {
    private const val USER_AGENT =
        "Mozilla/4.0 (compatible;MSIE 6.0;Windows NT 5.1;SV1;.NET CLR 1.1.4322;.NET CLR 2.0.50727;.NET CLR 3.0.04506.30)"
    private const val RESULT_HTML_ID = "div.result-container"

    // google translate does not support multiline text, so we need to track new lines somehow
    private const val NEW_LINE_REPLACEMENT = "_____"

    private const val MAX_TEXT_SIZE = 5000

    fun applyTranslationToMessage(msg: String, translation: String): String {
        return "$msg\r\n\r\n\uD83D\uDCAC GTranslate\r\n$translation"
    }

    fun translate(text: String, toLanguage: String, callback: (translatedText: String?) -> Unit) {
        val toLang = getLangCodeForGoogleTranslate(toLanguage)
        var resultText = ""
        val chunks = text.chunked(MAX_TEXT_SIZE)
        ioScope.launch {
            chunks.forEach { textChunk ->
                val url = TRANSLATE_URL.format(toLang, URLEncoder.encode(textChunk.replace("\n", NEW_LINE_REPLACEMENT), "utf-8"))

                try {
                    val document = Jsoup
                        .connect(url)
                        .userAgent(USER_AGENT)
                        .cookies(mapOf())
                        .get()

                    try {
                        val data = document.select(RESULT_HTML_ID).first().html()
                        resultText += data.replace("$NEW_LINE_REPLACEMENT ", "\n")
                            .replace(NEW_LINE_REPLACEMENT, "\n")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            uiScope.launch { callback(if (resultText.isEmpty()) null else resultText) }
        }
    }

    private fun getLangCodeForGoogleTranslate(shortName: String) : String {
        return when(shortName){
            "zh_hant_raw" -> "zh-TW"
            "zh_hans_raw" -> "zh-CN"
            "pt_br" -> "pt" // "Portuguese (Brazil)" isn't supported in googleTranslate
            else -> shortName
        }
    }
}