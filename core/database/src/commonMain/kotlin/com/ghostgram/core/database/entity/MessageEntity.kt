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
    val isRead: Boolean = false
)

const val TABLE_NAME = "messages"