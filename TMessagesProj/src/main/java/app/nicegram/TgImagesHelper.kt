package app.nicegram

import org.telegram.messenger.ContactsController
import org.telegram.messenger.DialogObject
import org.telegram.messenger.FileLoader
import org.telegram.messenger.ImageLocation
import org.telegram.messenger.MessageObject
import org.telegram.messenger.MessagesController
import org.telegram.messenger.UserConfig
import org.telegram.tgnet.TLRPC
import timber.log.Timber
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
            Timber.d("avatarsDebug imageLocation " + (if (imageLocation == null) "nul" else imageLocation.path))
            var attachPath: File? = null
            if (user.photo != null) {
                attachPath = fileLoader.getPathToAttach(user.photo.photo_small, true)
                Timber.d("avatarsDebug user attachPath$attachPath")
                if (!attachPath.exists()) fileLoader.loadFile(imageLocation, user, "jpg", 1, 1)
            }
            image = attachPath?.toString() ?: ""
        } else if (chat != null) {
            val imageLocation = ImageLocation.getForUserOrChat(chat, ImageLocation.TYPE_SMALL)
            Timber.d("avatarsDebug chat imageLocation " + (imageLocation ?: "nul"))
            var attachPath: File? = null
            if (chat.photo != null) {
                attachPath = fileLoader.getPathToAttach(chat.photo.photo_small, true)
                Timber.d("avatarsDebug chat attachPath$attachPath")
                if (!attachPath.exists()) fileLoader.loadFile(imageLocation, chat, "jpg", 1, 1)
            }
            image = attachPath?.toString() ?: ""
        }

        return image
    }
}