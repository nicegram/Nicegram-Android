package app.nicegram

import com.appvillis.feature_attention_economy.domain.entities.AttAdAction
import com.appvillis.feature_attention_economy.domain.entities.AttOngoingAction
import com.appvillis.feature_attention_economy.domain.entities.AttOngoingActionType
import com.appvillis.nicegram.NicegramAssistantEntryPoint
import dagger.hilt.EntryPoints
import kotlinx.coroutines.launch
import org.telegram.messenger.ApplicationLoader
import org.telegram.tgnet.TLRPC
import timber.log.Timber

object NicegramAttHelper {
    private fun entryPoint() =
        EntryPoints.get(ApplicationLoader.applicationContext, NicegramAssistantEntryPoint::class.java)

    fun needToShowCoinForJoin(chat: TLRPC.Chat?, user: TLRPC.User?): Boolean {
        return isOngoingAttAction(chat, user) != null
    }

    fun tryClaimSubscribe(chat: TLRPC.Chat?, user: TLRPC.User?) {
        try {
            val action = isOngoingAttAction(chat, user) ?: return
            val chatId = chat?.id ?: user?.id ?: return

            entryPoint().appScope().launch {
                entryPoint().claimAdsUseCase().invoke(AttAdAction.AttAdActionType.Subscribe, action.ad, chatId)
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private fun isOngoingAttAction(chat: TLRPC.Chat?, user: TLRPC.User?): AttOngoingAction? {
        val publicUsername = chat?.username ?: user?.username
        val ongoingActions = entryPoint().getOngoingActionsUseCase().invoke().value

        Timber.d("ongoing actions $ongoingActions, public username: $publicUsername")
        val foundAction = ongoingActions.find { it.type is AttOngoingActionType.Subscribe && it.type.getId() == publicUsername }

        Timber.d("found action $foundAction")

        return foundAction
    }
}