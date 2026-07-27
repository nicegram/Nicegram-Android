package app.nicegram.bridge

import com.appvillis.core_domain.usecase.telegramsession.IsSystemTelegramSessionUseCase
import com.appvillis.nicegram.NicegramAssistantEntryPoint
import dagger.hilt.EntryPoints
import org.telegram.messenger.ApplicationLoader

object TelegramSessionActiveSessionBridge {

    private fun entryPoint() = EntryPoints
        .get(ApplicationLoader.applicationContext, NicegramAssistantEntryPoint::class.java)

    /**
     * True when a session is a Nicegram-managed "system" session: its api_id is in
     * systemApiIds AND its device_model is in systemDeviceModels. Returns false on any failure.
     */
    @JvmStatic
    fun isSystemSession(apiId: Int, deviceModel: String?): Boolean = runCatching {
        entryPoint().isSystemTelegramSessionUseCase()
            .invoke(IsSystemTelegramSessionUseCase.Params(apiId, deviceModel))
    }.getOrDefault(false)
}
