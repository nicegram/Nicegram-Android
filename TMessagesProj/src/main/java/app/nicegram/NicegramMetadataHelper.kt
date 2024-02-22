package app.nicegram

import android.text.SpannableStringBuilder
import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.GsonBuilder
import org.telegram.messenger.MessageObject
import org.telegram.tgnet.TLRPC.*
import org.telegram.tgnet.TLObject

object NicegramMetadataHelper {
    private val excludedFieldsList = setOf(
        "mChangingConfigurations",
    )

    fun getMetadata(message: MessageObject): CharSequence {
        val gson = GsonBuilder()
            .setPrettyPrinting()
            .addSerializationExclusionStrategy(CustomExclusionStrategy(excludedFieldsList))
            .addDeserializationExclusionStrategy(CustomExclusionStrategy(excludedFieldsList))
            .create()

        val json = gson.toJson(message.messageOwner)
        val spanBuilder = SpannableStringBuilder(json)

        return spanBuilder
    }

    private class CustomExclusionStrategy(private val excludedFields: Set<String>) : ExclusionStrategy {
        override fun shouldSkipField(fAttr: FieldAttributes): Boolean {
            return excludedFields.contains(fAttr.name)
        }

        override fun shouldSkipClass(clazz: Class<*>?): Boolean {
            return false
        }
    }
}
