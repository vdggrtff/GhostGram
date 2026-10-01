package com.ghostgram.app.presentation.chats.chat_details.utils

import com.ghostgram.app.presentation.chats.chat_details.utils.MessageListItem.Album
import com.ghostgram.app.presentation.chats.chat_details.utils.MessageListItem.Single
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
                if (currentAlbum.isNotEmpty()) result.add(Album(currentAlbum))
                currentAlbumId = msg.mediaAlbumId
                currentAlbum = mutableListOf(msg)
            }
        } else {
            if (currentAlbum.isNotEmpty()) {
                result.add(Album(currentAlbum))
                currentAlbum = mutableListOf()
                currentAlbumId = 0L
            }
            result.add(Single(msg))
        }
    }
    if (currentAlbum.isNotEmpty()) result.add(Album(currentAlbum))
    return result.asReversed()
}

fun getDateFromItem(item: MessageListItem): Int = when (item) {
    is Single -> item.message.date
    is Album -> item.messages.first().date
}

fun getSenderKey(listItem: MessageListItem): Any {
    val msg = when (listItem) {
        is Single -> listItem.message
        is Album -> listItem.messages.first()
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