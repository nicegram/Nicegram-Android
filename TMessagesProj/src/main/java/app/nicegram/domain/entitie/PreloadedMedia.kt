package app.nicegram.domain.entitie

import java.io.File

typealias ChatIdWithMessageId = Pair<Long, Int>

data class PreloadedMedia(
    val messageId: Int,
    val chatId: Long,
    val mimeType: String,
    val file: File,
)
