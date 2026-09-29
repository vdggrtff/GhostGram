package com.ghostgram.app.presentation.chats.chat_details

import com.ghostgram.app.utils.TimeFormatter
import entity.Message

sealed class MessageListItem {
    data class Single(val message: Message) : MessageListItem()
    data class Album(val messages: List<Message>) : MessageListItem()
}

/**
 * 💥 Склеивает сообщения с одинаковым mediaAlbumId в единый альбом
 */
fun groupMessagesIntoAlbums(messages: List<Message>): List<MessageListItem> {
    val result = mutableListOf<MessageListItem>()
    var currentAlbumId = 0L
    var currentAlbum = mutableListOf<Message>()

    for (msg in messages) {
        if (msg.mediaAlbumId != 0L) {
            if (msg.mediaAlbumId == currentAlbumId) {
                currentAlbum.add(msg)
            } else {
                if (currentAlbum.isNotEmpty()) result.add(MessageListItem.Album(currentAlbum))
                currentAlbumId = msg.mediaAlbumId
                currentAlbum = mutableListOf(msg)
            }
        } else {
            if (currentAlbum.isNotEmpty()) {
                result.add(MessageListItem.Album(currentAlbum))
                currentAlbum = mutableListOf()
                currentAlbumId = 0L
            }
            result.add(MessageListItem.Single(msg))
        }
    }
    if (currentAlbum.isNotEmpty()) result.add(MessageListItem.Album(currentAlbum))
    return result.asReversed()
}

fun getDateFromItem(item: MessageListItem): Int = when (item) {
    is MessageListItem.Single -> item.message.date
    is MessageListItem.Album -> item.messages.first().date
}

fun getSenderKey(listItem: MessageListItem): Any {
    val msg = when (listItem) {
        is MessageListItem.Single -> listItem.message
        is MessageListItem.Album -> listItem.messages.first()
    }
    return when {
        msg.isOutgoing -> "MY_OUTGOING_MESSAGE"
        msg.senderId != 0L -> msg.senderId
        else -> msg.id
    }
}

/**
 * 💥 Проверяет, нужно ли показывать плашку даты над сообщением
 */
fun shouldShowDateHeader(index: Int, items: List<MessageListItem>): Boolean {
    if (index == items.size - 1) return true
    val currentItemDate = getDateFromItem(items[index])
    val olderItemDate = getDateFromItem(items[index + 1])
    return !TimeFormatter.isSameDay(currentItemDate, olderItemDate)
}