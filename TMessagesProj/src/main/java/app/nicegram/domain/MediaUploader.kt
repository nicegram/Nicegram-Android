package app.nicegram.domain

import org.telegram.messenger.MessageObject
import org.telegram.tgnet.TLRPC
import java.io.File

interface MediaUploader {
    suspend fun ensureMediaUploaded(message: MessageObject): File?
    suspend fun ensureMediaUploaded(message: TLRPC.Message, currentAccount: Int): File?
}
