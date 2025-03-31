package app.nicegram.bridge

import app.nicegram.TgImagesHelper
import com.appvillis.feature_keywords.domain.TgImagesLoader

class TgImagesLoaderImpl : TgImagesLoader {
    override fun getImgForDialog(dialogId: Long): String {
        return TgImagesHelper.getImgForDialog(dialogId)
    }
}