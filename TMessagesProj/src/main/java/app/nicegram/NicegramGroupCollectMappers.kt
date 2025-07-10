package app.nicegram

import com.appvillis.core_network.data.body.ChannelInfoRequest.MessageInformation
import com.appvillis.core_network.data.serialized.MediaWrapper
import org.telegram.messenger.MessageObject
import org.telegram.tgnet.TLObject
import org.telegram.tgnet.TLRPC
import timber.log.Timber

private const val GET_MESSAGE_COUNT_DEFAULT = 10

// region Public API

/**
 * Groups a list of [MessageObject]s by their `grouped_id` (or `id` if `grouped_id == 0`),
 * limits the number of groups to [getCount], and returns a list of pairs where each pair contains:
 * - a [MessageObject]
 * - a [Boolean] flag indicating whether the photo for this message should be loaded.
 *
 * The `shouldLoadPhoto` flag is set to `true` only for the first message in each group
 * that contains media of type [TLRPC.TL_messageMediaPhoto]. All other messages receive `false`.
 *
 * Groups are sorted by the date of their first message in descending order,
 * and only the [getCount] most recent groups are included.
 * Messages within each group are also sorted by date in descending order.
 * The final result list is sorted by message date in descending order.
 *
 * @param getCount The maximum number of message groups to include. Defaults to [GET_MESSAGE_COUNT_DEFAULT].
 *
 * @return A list of [Pair]<[MessageObject], [Boolean]>, where the Boolean indicates whether the photo should be loaded.
 */
internal fun List<MessageObject>.groupAndLimitMessageObjects(
    getCount: Int = GET_MESSAGE_COUNT_DEFAULT
): List<Pair<MessageObject, Boolean>> {
    return this
        .groupBy { it.messageOwner.grouped_id.takeIf { id -> id > 0 } ?: it.messageOwner.id.toLong() }
        .entries
        .sortedByDescending { it.value.firstOrNull()?.messageOwner?.date ?: 0 }
        .take(getCount)
        .flatMap { group ->
            val sortedGroup = group.value.sortedByDescending { it.messageOwner.date }

            val firstPhotoMessageIndex =
                sortedGroup.indexOfFirst { it.messageOwner.media is TLRPC.TL_messageMediaPhoto }

            sortedGroup.mapIndexed { index, messageObject ->
                val shouldLoadPhoto = index == firstPhotoMessageIndex
                messageObject to shouldLoadPhoto
            }
        }
        .sortedByDescending { (messageObject, needLoaded) ->  messageObject.messageOwner.date }
}

/**
 * Groups a list of [TLRPC.Message]s by their `grouped_id` (or `id` if `grouped_id == 0`),
 * limits the number of groups to [getCount], and returns a list of pairs where each pair contains:
 * - a [TLRPC.Message]
 * - a [Boolean] flag indicating whether the photo for this message should be loaded.
 *
 * The `shouldLoadPhoto` flag is set to `true` only for the first message in each group
 * that contains media of type [TLRPC.TL_messageMediaPhoto]. All other messages receive `false`.
 *
 * Groups are sorted by the date of their first message in descending order,
 * and only the [getCount] most recent groups are included.
 * Messages within each group are sorted by date in descending order.
 * The final result list is also sorted by message date in descending order.
 *
 * @param getCount The maximum number of message groups to include. Defaults to [GET_MESSAGE_COUNT_DEFAULT].
 * @return A list of [Pair]<[TLRPC.Message], [Boolean]>, or null if the input list is null.
 */
internal fun List<TLRPC.Message>?.groupAndLimitMessages(
    getCount: Int = GET_MESSAGE_COUNT_DEFAULT
): List<Pair<TLRPC.Message, Boolean>>? {
    if (this == null) return null

    return this
        .groupBy { msg -> msg.grouped_id.takeIf { it > 0 } ?: msg.id.toLong() }
        .entries
        .sortedByDescending { it.value.firstOrNull()?.date ?: 0 }
        .take(getCount)
        .flatMap { group ->
            val sortedGroup = group.value.sortedByDescending { it.date }

            val firstPhotoIndex = sortedGroup.indexOfFirst {
                it.media is TLRPC.TL_messageMediaPhoto
            }

            sortedGroup.mapIndexed { index, message ->
                val shouldLoadPhoto = index == firstPhotoIndex
                message to shouldLoadPhoto
            }
        }
        .sortedByDescending { (message, needLoaded) ->  message.date }
}

// endregion

// region Mapping Extensions

fun MessageObject.toMessageInformation(uploadId: Int?): MessageInformation? = try {
    val message = this.messageOwner

    MessageInformation(
        id = message.id,
        message = message.message ?: "",
        commentsCount = message.replies?.replies ?: 0,
        viewsCount = message.views,
        date = message.date,
        author = fromPeerObject?.toAuthor(),
        peerId = message.peer_id.toPeerId(),
        groupedId = message.grouped_id,
        reactions = message.reactions?.results?.mapNotNull { it.toReaction() } ?: emptyList(),
        media = message.media?.toMedia(uploadId)?.let { MediaWrapper.from(it) }
    )
} catch (e: Exception) {
    Timber.e(e)
    null
}

fun TLRPC.Message.toModel(
    chats: List<TLRPC.Chat>,
    users: List<TLRPC.User>,
    uploadId: Int?,
): MessageInformation? {
    if (this !is TLRPC.TL_message) return null

    return try {
        MessageInformation(
            id = id,
            message = message,
            commentsCount = replies?.replies ?: 0,
            viewsCount = views,
            date = date,
            author = from_id.toAuthor(chats, users),
            peerId = peer_id.toPeerId(),
            groupedId = grouped_id,
            reactions = reactions?.results?.mapNotNull { it.toReaction() } ?: emptyList(),
            media = media?.toMedia(uploadId)?.let { MediaWrapper.from(it) }
        )
    } catch (e: Exception) {
        Timber.e(e)
        null
    }
}

// endregion

// region Author Mapping

private fun TLObject.toAuthor(): MessageInformation.Author? = when (this) {
    is TLRPC.TL_channel -> MessageInformation.Author.AuthorChannel(
        id = id,
        title = title,
        username = username,
        usernames = usernames.map { it.username }
    )

    is TLRPC.TL_user -> MessageInformation.Author.AuthorUser(
        id = id,
        firstName = first_name,
        lastName = last_name,
        phone = phone,
        username = username,
        usernames = usernames.map { it.username }
    )

    is TLRPC.TL_chat -> MessageInformation.Author.AuthorChat(
        id = id,
        title = title,
        username = username,
        usernames = usernames.map { it.username }
    )

    else -> null
}

private fun TLRPC.Peer?.toAuthor(
    chats: List<TLRPC.Chat>,
    users: List<TLRPC.User>
): MessageInformation.Author? = when (this) {
    is TLRPC.TL_peerChannel -> chats.find { it.id == channel_id }?.let { channel ->
        MessageInformation.Author.AuthorChannel(
            id = channel.id,
            title = channel.title,
            username = channel.username,
            usernames = channel.usernames.map { it.username }
        )
    }

    is TLRPC.TL_peerChat -> chats.find { it.id == chat_id }?.let { chat ->
        MessageInformation.Author.AuthorChat(
            id = chat.id,
            title = chat.title,
            username = chat.username,
            usernames = chat.usernames.map { it.username }
        )
    }

    is TLRPC.TL_peerUser -> users.find { it.id == user_id }?.let { user ->
        MessageInformation.Author.AuthorUser(
            id = user.id,
            firstName = user.first_name,
            lastName = user.last_name,
            phone = user.phone,
            username = user.username,
            usernames = user.usernames.map { it.username }
        )
    }

    else -> null
}

// endregion

// region Peer ID Mapping

private fun TLRPC.Peer?.toPeerId(): Long = when (this) {
    is TLRPC.TL_peerUser -> user_id
    is TLRPC.TL_peerChat -> chat_id
    is TLRPC.TL_peerChannel -> channel_id
    else -> 0L
}

// endregion

// region Reactions Mapping

private fun TLRPC.ReactionCount?.toReaction(): MessageInformation.Reaction? {
    if (this !is TLRPC.TL_reactionCount) return null

    return when (val reaction = this.reaction) {
        is TLRPC.TL_reactionEmoji -> MessageInformation.Reaction.Emoji(
            emoticon = reaction.emoticon,
            count = count
        )

        is TLRPC.TL_reactionCustomEmoji -> MessageInformation.Reaction.CustomEmoji(
            documentId = reaction.document_id,
            count = count
        )

        is TLRPC.TL_reactionPaid -> MessageInformation.Reaction.Paid(
            count = count
        )

        else -> null
    }
}

// endregion

// region Media Mapping

private fun TLRPC.MessageMedia?.toMedia(uploadId: Int?): MessageInformation.Media? = when (this) {
    is TLRPC.TL_messageMediaPhoto -> {
        val photo = this.photo
        if (photo is TLRPC.TL_photo) {
            MessageInformation.Media.Photo(
                id = photo.id,
                hasStickers = photo.has_stickers,
                date = photo.date,
                sizes = photo.sizes.wrapPhotoSize(),
                videoSizes = photo.video_sizes.wrapVideoSize(),
                uploadId = uploadId
            )
        } else null
    }

    is TLRPC.TL_messageMediaDocument -> {
        val document = this.document
        if (document is TLRPC.TL_document) {
            document.attributes?.forEach { attr ->
                when (attr) {
                    is TLRPC.TL_documentAttributeAudio -> {
                        return MessageInformation.Media.Audio(
                            duration = attr.duration,
                            title = attr.title
                        )
                    }

                    is TLRPC.TL_documentAttributeVideo -> {
                        return MessageInformation.Media.Video(
                            duration = attr.duration
                        )
                    }
                }
            }
        }
        null
    }

    else -> null
}

// endregion

// region Photo/Video Size Mapping

private fun ArrayList<TLRPC.PhotoSize>.wrapPhotoSize(): ArrayList<MessageInformation.PhotoSize> {
    return this.mapNotNull { tlPhotoSize ->
        when (tlPhotoSize) {
            is TLRPC.TL_photoSizeEmpty -> MessageInformation.PhotoSize.PhotoSizeEmpty(tlPhotoSize.type)
            is TLRPC.TL_photoSize -> MessageInformation.PhotoSize.PhotoSize(
                type = tlPhotoSize.type,
                w = tlPhotoSize.w,
                h = tlPhotoSize.h,
                size = tlPhotoSize.size
            )

            is TLRPC.TL_photoCachedSize -> MessageInformation.PhotoSize.PhotoCachedSize(
                type = tlPhotoSize.type,
                w = tlPhotoSize.w,
                h = tlPhotoSize.h,
                bytes = tlPhotoSize.bytes
            )

            is TLRPC.TL_photoStrippedSize -> MessageInformation.PhotoSize.PhotoStrippedSize(
                type = tlPhotoSize.type,
                bytes = tlPhotoSize.bytes,
            )

            is TLRPC.TL_photoSizeProgressive -> MessageInformation.PhotoSize.PhotoSizeProgressive(
                type = tlPhotoSize.type,
                w = tlPhotoSize.w,
                h = tlPhotoSize.h,
                sizes = tlPhotoSize.sizes
            )

            is TLRPC.TL_photoPathSize -> MessageInformation.PhotoSize.PhotoPathSize(
                type = tlPhotoSize.type,
                bytes = tlPhotoSize.bytes
            )

            else -> null
        }
    }.toCollection(ArrayList())
}

private fun ArrayList<TLRPC.VideoSize>.wrapVideoSize(): ArrayList<MessageInformation.VideoSize> {
    return this.mapNotNull { tlVideoSize ->
        when (tlVideoSize) {
            is TLRPC.TL_videoSize -> MessageInformation.VideoSize.VideoSize(
                type = tlVideoSize.type,
                w = tlVideoSize.w,
                h = tlVideoSize.h,
                size = tlVideoSize.size
            )

            is TLRPC.TL_videoSizeEmojiMarkup -> MessageInformation.VideoSize.VideoSizeEmojiMarkup(
                emojiId = tlVideoSize.emoji_id,
                backgroundsColors = tlVideoSize.background_colors
            )

            is TLRPC.TL_videoSizeStickerMarkup -> MessageInformation.VideoSize.VideoSizeStickerMarkup(
                stickerId = tlVideoSize.sticker_id,
                backgroundsColors = tlVideoSize.background_colors
            )

            else -> null
        }
    }.toCollection(ArrayList())
}

// endregion