package app.nicegram.bridge

import android.graphics.Bitmap
import android.graphics.Color
import com.appvillis.core_network.domain.UserLocaleProvider
import com.appvillis.feature_auth.domain.TelegramIdBridge
import com.appvillis.nicegram_wallet.module_bridge.ContactMessageSender
import com.appvillis.nicegram_wallet.module_bridge.QrCodeRenderer
import com.appvillis.nicegram_wallet.wallet_contacts.domain.ContactsRetriever
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.telegram.messenger.LocaleController
import org.telegram.messenger.UserConfig
import java.util.Locale
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TgBridgeModule {
    @Provides
    fun provideContactsRetriever(): ContactsRetriever = ContactsRetrieverImpl()

    @Provides
    fun provideContactMessageSender(): ContactMessageSender = ContactMessageSenderImpl()

    @Provides
    @Singleton
    fun provideQrCodeRenderer(): QrCodeRenderer = object : QrCodeRenderer {
        override fun getQrBitmapByString(s: String, width: Int, height: Int): Bitmap? {
            try {
                val writer = QRCodeWriter()
                val hints = hashMapOf<EncodeHintType, Any>(
                    EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
                    EncodeHintType.MARGIN to 0
                )

                val bitMatrix = writer.encode(s, BarcodeFormat.QR_CODE, width, height, hints)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val startColor = Color.parseColor("#FFBE55ED")
                val endColor = Color.parseColor("#FF4EACF3")
                for (x in 0 until width) {
                    for (y in 0 until height) {
                        bitmap.setPixel(
                            x,
                            y,
                            if (bitMatrix[x, y]) calculateGradientColor(
                                startColor,
                                endColor,
                                x,
                                y,
                                width,
                                height
                            ) else Color.TRANSPARENT
                        )
                    }
                }
                return bitmap
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }
    }

    @Provides
    @Singleton
    fun provideUserLocaleProvider(): UserLocaleProvider = object : UserLocaleProvider {
        override val lang: String?
            get() = try {
                LocaleController.getInstance().currentLocale?.language
            } catch (e: Exception) {
                Locale.getDefault().language
            } catch (e: UnsatisfiedLinkError) {
                Locale.getDefault().language
            }
    }

    @Provides
    @Singleton
    fun provideTgIdBridge() = object : TelegramIdBridge {
        override val telegramId: Long
            get() = UserConfig.getInstance(UserConfig.selectedAccount).clientUserId
    }
}