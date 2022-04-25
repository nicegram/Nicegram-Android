package com.appvillis.nicegram

import com.appvillis.nicegram.NicegramConstsAndKeys.TRANSLATE_URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jsoup.Jsoup

object NicegramTranslator {
    private const val USER_AGENT = "Mozilla/4.0 (compatible;MSIE 6.0;Windows NT 5.1;SV1;.NET CLR 1.1.4322;.NET CLR 2.0.50727;.NET CLR 3.0.04506.30)"
    private const val RESULT_HTML_ID = "div.result-container"

    // google translate does not support multiline text, so we need to track new lines somehow
    private const val NEW_LINE_REPLACEMENT = "_____"

    fun applyTranslationToMessage(msg: String, translation: String): String {
        return "$msg\r\n\r\n\uD83D\uDCAC GTranslate\r\n$translation"
    }

    fun translate(text: String, toLanguage: String, callback: (translatedText: String?) -> Unit) {
        val url = TRANSLATE_URL.format(toLanguage, text.replace("\n", NEW_LINE_REPLACEMENT))

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val document = Jsoup
                    .connect(url)
                    .userAgent(USER_AGENT)
                    .cookies(mapOf())
                    .get()

                GlobalScope.launch(Dispatchers.Main) {
                    try {
                        val data = document.select(RESULT_HTML_ID).first().html()
                        callback(
                            data.replace("$NEW_LINE_REPLACEMENT ", "\n")
                                .replace(NEW_LINE_REPLACEMENT, "\n")
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        callback(null)
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                GlobalScope.launch(Dispatchers.Main) { callback(null) }
            }
        }
    }
}