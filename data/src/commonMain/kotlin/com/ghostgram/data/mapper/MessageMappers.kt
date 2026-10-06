package com.ghostgram.data.mapper

import com.ghostgram.core.database.entity.MessageEntity
import entity.Message
import entity.MessageMediaType

fun MessageEntity.toDomain(): Message = Message(
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
    waveform = waveform
)