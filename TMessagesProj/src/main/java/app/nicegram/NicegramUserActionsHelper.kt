package app.nicegram

import com.appvillis.feature_nicegram_client.NicegramClientHelper
import com.appvillis.nicegram.NicegramAssistantEntryPoint
import com.appvillis.rep_user_actions.domain.entities.AttUserAction
import dagger.hilt.EntryPoints
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.UserConfig
import timber.log.Timber
import java.util.UUID

object NicegramUserActionsHelper {
    private fun entryPoint() =
        EntryPoints.get(ApplicationLoader.applicationContext, NicegramAssistantEntryPoint::class.java)

    fun saveActionIfAllowed(chatId: Long, action: AttUserAction.AttUserActionType) {
        try {
            entryPoint().saveUserActionUseCase().invoke(
                AttUserAction(
                    id = UUID.randomUUID().toString(),
                    chatId = NicegramClientHelper.preparedChatId(chatId),
                    timestamp = System.currentTimeMillis(),
                    type = action,
                    userId = UserConfig.getInstance(UserConfig.selectedAccount).clientUserId
                )
            )
        } catch (e: Exception) {
            Timber.e(e)
        }
    }
}