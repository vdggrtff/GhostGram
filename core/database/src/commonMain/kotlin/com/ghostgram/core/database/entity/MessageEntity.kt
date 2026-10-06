package com.ghostgram.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = TABLE_NAME)
data class MessageEntity(
    @PrimaryKey
    val id: Long, // ID сообщения из Telegram
    val chatId: Long,
    val senderName: String,
    val text: String,
    val isOutgoing: Boolean,
    val isDeletedLocally: Boolean = false,
    val photoPath: String? = null,
    val mediaType: String = "TEXT",
    val fileName: String? = null,
    val fileExtraInfo: String? = null,
    val isRead: Boolean = false,
    val date: Int = 0,
    val mediaAlbumId: Long = 0L,
    val isSending: Boolean = false,
    val replyToMessageId: Long = 0L,
    val isEdited: Boolean = false,
    val senderId: Long = 0L,
    val senderAvatarPath: String? = null,
    val waveform: String? = null
)

const val TABLE_NAME = "messages"