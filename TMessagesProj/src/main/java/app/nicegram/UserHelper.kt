package app.nicegram

import app.nicegram.bridge.TgBridgeEntryPoint
import com.appvillis.nicegram.NicegramAssistantEntryPoint
import dagger.hilt.EntryPoints
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.telegram.messenger.ApplicationLoader

object UserHelper { // TODO [TECH_DEBT] need to think how to manage such class in pretty way

    private fun entryPoint() =
        EntryPoints.get(ApplicationLoader.applicationContext, NicegramAssistantEntryPoint::class.java)

    private fun tgBridgeEntryPoint() =
        EntryPoints.get(ApplicationLoader.applicationContext, TgBridgeEntryPoint::class.java)

    fun observeFirstUserSignIn() {
        tgBridgeEntryPoint()
            .hasSignedInUseCase()
            .getFirstAccountAuthFlow()
            .onEach { isAuthorized ->
                entryPoint().checkIfNeedToCompleteAutoLoginUseCase().invoke(isTelegramUserAuthorized = isAuthorized)
            }
            .launchIn(entryPoint().appScope())
    }
}
