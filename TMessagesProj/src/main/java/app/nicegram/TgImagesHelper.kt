package app.nicegram

import org.telegram.messenger.DialogObject
import org.telegram.messenger.FileLoader
import org.telegram.messenger.ImageLoader
import org.telegram.messenger.ImageLocation
import org.telegram.messenger.MessageObject
import org.telegram.messenger.MessagesController
import org.telegram.messenger.UserConfig
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.TLRPC.TL_messageMediaPhoto
import java.io.File

object TgImagesHelper {
    fun getImgForDialog(dialogId: Long): String {
        val currentAccount = UserConfig.selectedAccount
        val fileLoader = FileLoader.getInstance(currentAccount)

        var image = ""
        var user: TLRPC.User? = null
        var chat: TLRPC.Chat? = null
        if (DialogObject.isUserDialog(dialogId)) {
            user = MessagesController.getInstance(currentAccount).getUser(dialogId)
        } else if (DialogObject.isChatDialog(dialogId)) {
            chat = MessagesController.getInstance(currentAccount).getChat(-dialogId);
        }

        if (user != null) {
            val imageLocation = ImageLocation.getForUserOrChat(user, ImageLocation.TYPE_SMALL)
            var attachPath: File? = null
            if (user.photo != null) {
                attachPath = fileLoader.getPathToAttach(user.photo.photo_small, true)
                if (!attachPath.exists()) fileLoader.loadFile(imageLocation, user, "jpg", 1, 1)
            }
            image = attachPath?.toString() ?: ""
        } else if (chat != null) {
            val imageLocation = ImageLocation.getForUserOrChat(chat, ImageLocation.TYPE_SMALL)
            var attachPath: File? = null
            if (chat.photo != null) {
                attachPath = fileLoader.getPathToAttach(chat.photo.photo_small, true)
                if (!attachPath.exists()) fileLoader.loadFile(imageLocation, chat, "jpg", 1, 1)
            }
            image = attachPath?.toString() ?: ""
        }

        return image
    }

    fun getFileForMessagePhoto(messageObject: MessageObject, withLoad: Boolean = true): File? {
        val media = messageObject.messageOwner.media
        if (media is TL_messageMediaPhoto) {
            val photo: TLRPC.Photo? = media.photo
            if (photo != null && photo.sizes.isNotEmpty()) {
//                val photoSize: TLRPC.PhotoSize? = FileLoader.getClosestPhotoSizeWithSize(photo.sizes, 800)  // type=x 800x800
                val photoSize: TLRPC.PhotoSize? = photo.sizes.find { it.type == "x" }
                if (photoSize != null) {
                    val currentAccount = UserConfig.selectedAccount
                    val fileLoader = FileLoader.getInstance(currentAccount)

                    val imageLocation = ImageLocation.getForPhoto(photoSize, photo)

                    val file: File = fileLoader.getPathToAttach(photoSize, true)

                    if (!file.exists() && withLoad) {
                        fileLoader.loadFile(
                            imageLocation,
                            messageObject.messageOwner,
                            "jpg",
                            FileLoader.PRIORITY_NORMAL,
                            ImageLoader.CACHE_TYPE_CACHE
                        )
                    }

                    return file
                }
            }
        }

        return null
    }
}
