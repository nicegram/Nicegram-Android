package app.nicegram.bridge

import com.appvillis.feature_user_activities.domain.UserCommonGroupsMessagesRetriever
import com.appvillis.feature_user_activities.domain.entities.UserActivityMessage
import org.telegram.messenger.MessagesController
import org.telegram.messenger.UserConfig
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.TLRPC.TL_inputMessagesFilterEmpty
import org.telegram.tgnet.TLRPC.messages_Messages
import timber.log.Timber
import kotlin.coroutines.suspendCoroutine

class UserCommonGroupsMessagesRetrieverImpl :
    UserCommonGroupsMessagesRetriever {
    override suspend fun getMessagesInCommonGroups(userId: Long): List<UserActivityMessage> {
        return suspendCoroutine { continuation ->
            val result = mutableListOf<UserActivityMessage>()
            val currentAccount = UserConfig.selectedAccount
            val messagesController = MessagesController.getInstance(currentAccount)
            val req = TLRPC.TL_messages_getCommonChats().apply {
                user_id = messagesController.getInputUser(userId)
                limit = 100
                max_id = 0
            }

            ConnectionsManager.getInstance(currentAccount).sendRequest(req) { response, error ->
                if (error == null) {
                    val chats = response as TLRPC.messages_Chats
                    if (chats.chats.isEmpty()) {
                        continuation.resumeWith(Result.success(listOf()))
                        return@sendRequest
                    }
                    var requestsFinished = 0
                    chats.chats.forEach { chat ->
                        Timber.d("common chat id:${chat.id} title:${chat.title}")
                        val msgReq = TLRPC.TL_messages_search().apply {
                            this.limit = 50
                            this.from_id = messagesController.getInputPeer(userId)
                            this.flags = this.flags or 1
                            this.peer = MessagesController.getInputPeer(chat)
                            this.filter = TL_inputMessagesFilterEmpty()
                        }
                        ConnectionsManager.getInstance(currentAccount).sendRequest(msgReq) { response, error ->
                            Timber.d("response for chat ${chat.title} " + response)
                            requestsFinished++
                            if (error == null) {
                                if (response is messages_Messages) {
                                    val messages = response

                                    messages.messages.forEach { message ->
                                        if (!message.message.isNullOrEmpty()) {
                                            result.add(
                                                UserActivityMessage(
                                                    message.id.toLong(),
                                                    -chat.id,
                                                    chat.title,
                                                    message.date * 1000L,
                                                    message.message
                                                )
                                            )
                                        }
                                    }
                                }
                            } else {
                                Timber.d("error is $error")
                            }

                            if (requestsFinished == chats.chats.size) {
                                continuation.resumeWith(Result.success(result))
                            }
                        }
                    }
                }
            }

        }
    }
}