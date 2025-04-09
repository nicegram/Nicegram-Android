package app.nicegram.bridge

import app.nicegram.TgImagesHelper
import com.appvillis.core_resources.domain.TgImagesLoader

class TgImagesLoaderImpl : TgImagesLoader {
    override fun getImgForDialog(dialogId: Long): String {
        return TgImagesHelper.getImgForDialog(dialogId)
    }
}