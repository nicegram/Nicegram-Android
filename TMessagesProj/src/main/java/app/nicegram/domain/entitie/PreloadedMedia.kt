package app.nicegram.domain.entitie

import java.io.File

data class PreloadedMedia(
    val messageId: Long,
    val chatId: Long,
    val mimeType: String,
    val file: File,
)
