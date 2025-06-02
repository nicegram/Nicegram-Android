package app.nicegram

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.appvillis.assistant_core.MainActivity
import com.appvillis.feature_ai_chat_analysis.AiAnalysisEntryPoint
import com.appvillis.feature_ai_chat_analysis.domain.entities.Session
import com.appvillis.feature_ai_chat_analysis.domain.entities.SourceData
import com.appvillis.feature_ai_chat_analysis.domain.features.AiChatAnalysisSource
import com.appvillis.feature_ai_chat_analysis.domain.features.ChatFilter
import com.appvillis.feature_ai_chat_analysis.presentation.AiChatAnalysisFragmentArgs
import com.appvillis.feature_ai_chat_analysis.presentation.SessionPickBottomSheetFragment.Companion.create
import com.appvillis.feature_ai_chat_analysis.presentation.SessionsListFragmentArgs
import com.appvillis.nicegram.AnalyticsHelper
import dagger.hilt.EntryPoints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.MessageObject
import org.telegram.messenger.MessagesStorage
import org.telegram.messenger.R
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.TLRPC.Chat
import org.telegram.tgnet.TLRPC.Message
import org.telegram.tgnet.TLRPC.User
import timber.log.Timber
import kotlin.coroutines.suspendCoroutine

object AiAnalysisHelper {
    fun onChatAnalysisClick(fromContextMenu: Boolean, activity: Activity, currentAccount: Int, isTopic: Boolean, threadMessageId: Long, currentChat: Chat?, currentUser: User?, messages: List<MessageObject>) {
        AnalyticsHelper.logEvent(activity, if (fromContextMenu) "chat_ai_open_from_menu" else "chat_ai_open_from_chat", null)

        val currentId: Long = if ((currentChat != null)) currentChat.id else currentUser?.id ?: 0
        GlobalScope.launch(Dispatchers.IO) {
            val bundle = getAndPrepareMessages(currentAccount, isTopic, threadMessageId, messages, currentChat, currentUser)

            withContext(Dispatchers.Main) {
                if (hasSessionsForChatId(currentId)) {
                    val frag = create(currentId, { session: Session? ->
                        openSession(
                            activity,
                            session!!
                        )
                    }, {
                        openHistory(activity, currentId)
                    }, {
                        openNewChat(activity, bundle)
                    })

                    frag.show((activity as FragmentActivity).supportFragmentManager, null)
                } else {
                    openNewChat(activity, bundle)
                }
            }
        }

    }

    private fun hasSessionsForChatId(chatId: Long): Boolean {
        val useCase = EntryPoints.get(
            ApplicationLoader.applicationContext,
            AiAnalysisEntryPoint::class.java
        ).getSessionsUseCase()

        return useCase().any { it.source.chatId == chatId }
    }

    private fun openHistory(activity: Activity, chatId: Long) {
        MainActivity.launchRoute(
            activity, R.id.action_global_sessionsListFragmentPop, SessionsListFragmentArgs(
                chatFilter = ChatFilter(setOf(chatId))
            ).toBundle()
        )
    }

    private fun openSession(activity: Activity, session: Session) {
        MainActivity.launchRoute(
            activity, R.id.action_global_aiChatAnalysisFragmentPop, AiChatAnalysisFragmentArgs(
                chat = null,
                session = AiChatAnalysisSource.Session(session)
            ).toBundle()
        )
    }

    private fun openNewChat(activity: Activity, bundle: Bundle) {
        MainActivity.launchRoute(activity, R.id.action_global_aiChatAnalysisFragmentPop, bundle)
    }

    suspend fun loadMessages(currentAccount: Int, isTopic: Boolean, threadMessageId: Long, chat: Chat?, user: User?): List<Message> {
        Timber.d("loadMessages for currentAccount: $currentAccount chat:${chat?.id} user:${user?.id}")
        val messages = suspendCoroutine<List<Message>> { continuation ->
            val id = (if (chat != null) -chat.id else user?.id) ?: 0
            MessagesStorage.getInstance(currentAccount).getMessagesInternal(
                id,
                0,
                200,
                0,
                0,
                0,
                0,
                0,
                0,
                threadMessageId,
                0,
                true,
                isTopic,
                null,
                object : MessagesStorage.GetMessagesValueCallback {
                    override fun onResult(res: TLRPC.TL_messages_messages) {
                        Timber.d("loaded messages ${res.messages?.size ?: -1}, first msg is: ${res.messages?.getOrNull(0)?.message ?: "NULL"}")
                        continuation.resumeWith(Result.success(res.messages))
                    }
                }
            )
        }

        return messages
    }

    private suspend fun getAndPrepareMessages(currentAccount: Int, isTopic: Boolean, threadMessageId: Long, messages: List<MessageObject>, chat: Chat?, user: User?): Bundle {
        Timber.d("messages amount ${messages.size}")

        try {
            val maxMessages = 100

            var chatFullName = chat?.title ?: chat?.username
            val chatUsernameName = chat?.username ?: user?.username
            var userLastName = user?.last_name
            var userFirstName = user?.first_name
            if (userLastName == "null") userLastName = ""
            if (userFirstName == "null") userFirstName = ""
            if (chatFullName == null && user != null) chatFullName = getUserDisplayName(userFirstName, userLastName)

            val loadedMessages = loadMessages(currentAccount, isTopic, threadMessageId, chat, user)
            val source = AiChatAnalysisSource.Chat(
                sourceData = SourceData(
                    chatId = chat?.id ?: user?.id ?: -1,
                    chatFullname = chatFullName ?: "",
                    chatUsername = chatUsernameName ?: "",
                    /*
                    messages = loadedMessages.filter { it.messageOwner != null }.sortedBy { it.messageOwner.date }.takeLast(maxMessages).mapNotNull {
                        val messageData = it.getText(currentAccount, chat) ?: return@mapNotNull null
                        SourceData.Message(messageData.displayName, it.messageOwner.date * 1000L, messageData.text)
                    }
                     */
                    messages = loadedMessages.sortedBy { it.date }.takeLast(maxMessages).mapNotNull {
                        val messageData = it.getText(currentAccount, chat) ?: return@mapNotNull null
                        SourceData.Message(messageData.displayName, it.date * 1000L, messageData.text)
                    }
                )
            )

            return AiChatAnalysisFragmentArgs(chat = source, session = null).toBundle()
        } catch (e: Exception) {
            Timber.e(e)
            throw e
        }
    }

    private fun MessageObject.getText(currentAccount: Int, chat: Chat?): MessageData? {
        val it = this
        if (it.isDateObject) return null
        val from = it.messageOwner.from_id
        var displayName = ""
        var msgText = it.messageText.toString()
        if (from is TLRPC.TL_peerUser) {
            val msgSender =
                MessagesStorage.getInstance(currentAccount).getUser(it.messageOwner.from_id.user_id)
            displayName = getUserDisplayName(msgSender.first_name, msgSender.last_name)
        } else if (chat != null) {
            displayName = chat.title
        }
        if (it.messageOwner.media != null) {
            val media = it.messageOwner.media
            if (media.document != null) {
                msgText =
                    "File_attached_${media.document.file_name_fixed?.toString() ?: media.document.mime_type}"
            }
        }
        val captionPrefix = if (it.caption != null) "${it.caption} " else ""
        if (it.type == MessageObject.TYPE_PHOTO) {
            msgText = "$captionPrefix[Image_attached]"
        }
        if (it.replyMessageObject != null) {
            val replyText = it.replyMessageObject.getText(currentAccount, chat)
            if (replyText != null) msgText += " (Reply to: ${replyText.text})"
        }
        return MessageData(msgText, displayName)
    }

    private fun Message.getText(currentAccount: Int, chat: Chat?): MessageData? {
        if (this is TLRPC.TL_messageService) return null
        val it = this
        val from = it.from_id
        var displayName = ""
        var msgText = it.message ?: ""
        if (from is TLRPC.TL_peerUser) {
            val msgSender =
                MessagesStorage.getInstance(currentAccount).getUser(it.from_id.user_id)
            displayName = getUserDisplayName(msgSender.first_name, msgSender.last_name)
        } else if (chat != null) {
            displayName = chat.title
        }
        if (it.media != null) {
            val media = it.media
            if (media.document != null) {
                var fileName = media.document.file_name_fixed?.toString()
                if (fileName.isNullOrEmpty()) fileName = media.document.mime_type
                if (msgText.isNotEmpty()) msgText += " "
                msgText +=
                    "[File_attached_$fileName]"
            }
        }

        val captionPrefix = if (it.message != null && it.message.isNotEmpty()) "${it.message} " else ""
        if (it.media != null && it.media.photo != null) {
            msgText = "$captionPrefix[Image_attached]"
        }
        if (it.replyMessage != null) {
            val replyText = it.replyMessage.getText(currentAccount, chat)
            if (replyText != null) msgText += " (Reply to: ${replyText.text})"
        }
        return MessageData(msgText, displayName)
    }

    class MessageData(val text: String, val displayName: String)

    private fun getUserDisplayName(firstName: String?, lastName: String?): String {
        var userLastName = firstName ?: ""
        var userFirstName = lastName ?: ""
        if (userLastName == "null") userLastName = ""
        if (userFirstName == "null") userFirstName = ""
        return ("$userFirstName $userLastName").trim()
    }
}