package com.ghostgram.data.mapper

import com.ghostgram.core.database.entity.MessageEntity
import entity.Message
import entity.MessageMediaType
import entity.MessageReactionItem
import kotlinx.serialization.json.Json
import kotlin.collections.emptyList

private val jsonHelper = Json { ignoreUnknownKeys = true }

fun MessageEntity.toDomain(): Message {
    val parsedReactions = if (reactionsJson.isNotBlank()) {
        try {
            val list = jsonHelper.decodeFromString<List<MessageReactionItem>>(reactionsJson)
            // Раскомментируй для проверки, если хочешь видеть в логах:
            // println("❤️ [MAPPER OK] msg=$id: успешно распаковано ${list.size} реакций!")
            list
        } catch (e: Exception) {
            println("❌ [MAPPER ERROR] Ошибка десериализации реакций msg=$id: ${e.message} | Сырая строка: '$reactionsJson'")
            emptyList()
        }
    } else {
        emptyList()
    }
    return Message(
        id = id,
        chatId = chatId,
        senderName = senderName,
        text = text,
        isOutgoing = isOutgoing,
        isDeletedLocally = isDeletedLocally,
        photoPath = photoPath,
        mediaType = runCatching { MessageMediaType.valueOf(mediaType) }.getOrDefault(MessageMediaType.TEXT),
        fileName = fileName,
        fileExtraInfo = fileExtraInfo,
        isRead = isRead,
        date = date,
        mediaAlbumId = mediaAlbumId,
        isSending = isSending,
        replyToMessageId = replyToMessageId,
        isEdited = isEdited,
        senderId = senderId,
        senderAvatarPath = senderAvatarPath,
        waveform = waveform,
        reactions = parsedReactions
    )
}
