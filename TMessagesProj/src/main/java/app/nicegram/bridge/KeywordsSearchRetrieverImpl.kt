package app.nicegram.bridge

import android.text.TextUtils
import androidx.collection.LongSparseArray
import com.appvillis.feature_keywords.domain.KeywordsSearchRetriever
import com.appvillis.feature_keywords.domain.entities.KeywordsMessage
import org.telegram.messenger.ContactsController
import org.telegram.messenger.DialogObject
import org.telegram.messenger.MessageObject
import org.telegram.messenger.MessagesController
import org.telegram.messenger.MessagesStorage
import org.telegram.messenger.UserConfig
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLObject
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.TLRPC.Chat
import org.telegram.tgnet.TLRPC.TL_error
import org.telegram.tgnet.TLRPC.TL_inputMessagesFilterEmpty
import org.telegram.tgnet.TLRPC.TL_inputPeerEmpty
import org.telegram.tgnet.TLRPC.TL_messages_searchGlobal
import org.telegram.tgnet.TLRPC.messages_Messages
import timber.log.Timber
import kotlin.coroutines.suspendCoroutine

class KeywordsSearchRetrieverImpl : KeywordsSearchRetriever {
    override suspend fun getMessages(keyword: String): List<KeywordsMessage> {
        val keywordMessages: MutableList<KeywordsMessage> = ArrayList()

        val query = keyword

        val req = TL_messages_searchGlobal()
        req.broadcasts_only = false
        req.groups_only = false
        req.users_only = false
        req.limit = 20
        req.q = query
        req.filter = TL_inputMessagesFilterEmpty()
        req.flags = req.flags or 1
        req.offset_rate = 0
        req.offset_id = 0
        req.offset_peer = TL_inputPeerEmpty()

        val currentAccount = UserConfig.selectedAccount
        return suspendCoroutine { continuation ->
            ConnectionsManager.getInstance(currentAccount).sendRequest(req, { response: TLObject, error: TL_error? ->
                if (error != null) {
                    continuation.resumeWith(Result.success(listOf()))
                    return@sendRequest
                }
                try {
                    val messageObjects = ArrayList<MessageObject>()
                    val res = response as messages_Messages

                    val chatsMap = LongSparseArray<Chat>()
                    val usersMap = LongSparseArray<TLRPC.User>()
                    for (a in res.chats.indices) {
                        val chat = res.chats[a]
                        chatsMap.put(chat.id, chat)
                    }
                    for (a in res.users.indices) {
                        val user = res.users[a]
                        usersMap.put(user.id, user)
                    }
                    for (a in res.messages.indices) {
                        val message = res.messages[a]
                        val messageObject = MessageObject(currentAccount, message, usersMap, chatsMap, false, true)
                        messageObjects.add(messageObject)
                        messageObject.setQuery(query)
                    }

                    val searchResultMessages = java.util.ArrayList<MessageObject>()
                    MessagesStorage.getInstance(currentAccount)
                        .putUsersAndChats(res.users, res.chats, true, true)
                    MessagesController.getInstance(currentAccount).putUsers(res.users, false)
                    MessagesController.getInstance(currentAccount).putChats(res.chats, false)
                    if (req.offset_id == 0) {
                        searchResultMessages.clear()
                    }
                    for (a in res.messages.indices) {
                        val message = res.messages[a]
                        val did = MessageObject.getDialogId(message)
                        val maxId = MessagesController.getInstance(currentAccount).deletedHistory[did]
                        if (maxId != 0 && message.id <= maxId) {
                            continue
                        }
                        val msg = messageObjects[a]
                        searchResultMessages.add(msg)
                        val dialog_id = MessageObject.getDialogId(message)
                        val read_max =
                            if (message.out) MessagesController.getInstance(currentAccount).dialogs_read_outbox_max else MessagesController.getInstance(
                                currentAccount
                            ).dialogs_read_inbox_max
                        val value = read_max[dialog_id]
                        if (value != null) {
                            message.unread = value < message.id
                        }
                    }
                    for (msg in searchResultMessages) {
                        val dialogId = msg.dialogId
                        var name: String? = null
                        if (DialogObject.isUserDialog(dialogId)) {
                            val user = MessagesController.getInstance(currentAccount).getUser(dialogId)
                            name = ContactsController.formatName(user)
                        } else if (DialogObject.isChatDialog(dialogId)) {
                            val chat = MessagesController.getInstance(currentAccount).getChat(-dialogId)
                            name = chat.title
                        }

                        if (name != null) {
                            val text: String = if (!TextUtils.isEmpty(msg.caption)) {
                                msg.caption.toString()
                            } else {
                                msg.messageText.toString()
                            }

                            keywordMessages.add(
                                KeywordsMessage(
                                    msg.id.toString(),
                                    text,
                                    name,
                                    msg.dialogId,
                                    msg.messageOwner.date * 1000L,
                                    ""
                                )
                            )
                        }

                    }

                    continuation.resumeWith(Result.success(keywordMessages))
                } catch (e: Exception) {
                    Timber.e(e)
                    continuation.resumeWith(Result.success(listOf()))
                }

            }, ConnectionsManager.RequestFlagFailOnServerErrors)
        }
    }
}