package app.nicegram.bridge

import com.appvillis.feature_auth.domain.TelegramBotBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.telegram.messenger.UserConfig
import org.telegram.messenger.Utilities
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLRPC
import timber.log.Timber
import kotlin.coroutines.suspendCoroutine

class TelegramBotBridgeImpl : TelegramBotBridge {
    override suspend fun sendStartMessage(botUsername: String, msg: String) {
        val currentAccount = UserConfig.selectedAccount

        withContext(Dispatchers.IO) {
            suspendCoroutine { continuation ->
                try {
                    val connectionsManager = ConnectionsManager.getInstance(currentAccount)
                    val botResolveRequest = TLRPC.TL_contacts_resolveUsername().apply {
                        this.username = botUsername
                    }

                    connectionsManager.sendRequest(botResolveRequest) { response, error ->
                        if (error != null) {
                            Timber.e("${error.code} ${error.text}")
                            continuation.resumeWith(Result.success(false))
                            return@sendRequest
                        }

                        val accessHash = (response as TLRPC.TL_contacts_resolvedPeer).users[0].access_hash
                        val id = response.users[0].id

                        val startReq = TLRPC.TL_messages_startBot().apply {
                            start_param = msg
                            random_id = Utilities.random.nextLong()
                            peer = TLRPC.TL_inputPeerUser().apply {
                                this.user_id = id
                                this.access_hash = accessHash
                            }
                            bot = TLRPC.TL_inputUser().apply {
                                this.user_id = id
                                this.access_hash = accessHash
                            }
                        }

                        connectionsManager.sendRequest(startReq) startReq@ { _, startError ->
                            if (startError != null) {
                                Timber.e("${startError.code} ${startError.text}")
                                continuation.resumeWith(Result.success(false))
                                return@startReq
                            }
                            continuation.resumeWith(Result.success(true))
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e)
                    continuation.resumeWith(Result.success(false))
                }
            }
        }
    }
}