package com.ghostgram.data.repository.utils

import com.ghostgram.core.tdlib.TelegramFlowClient
import entity.Message
import entity.MessageMediaType
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

class TdlibMessageParser(
    private val tdlibClient: TelegramFlowClient,
    private val tracker: DownloadTracker
) {
    fun parse(msgObj: JsonObject, fallbackChatId: Long = 0L): Message? {
        val msgId = msgObj["id"]?.jsonPrimitive?.longOrNull ?: return null
        val chatId = msgObj["chat_id"]?.jsonPrimitive?.longOrNull ?: fallbackChatId
        val isOutgoing = msgObj["is_outgoing"]?.jsonPrimitive?.booleanOrNull ?: false
        val date = msgObj["date"]?.jsonPrimitive?.intOrNull ?: 0
        val isSending = msgObj["sending_state"] != null
        val editDate = msgObj["edit_date"]?.jsonPrimitive?.intOrNull ?: 0
        val mediaAlbumId = msgObj["media_album_id"]?.jsonPrimitive?.longOrNull ?: 0L

        val replyToObj = msgObj["reply_to"]?.jsonObject
        val replyToId = if (replyToObj?.get("@type")?.jsonPrimitive?.content == "messageReplyToMessage") {
            replyToObj["message_id"]?.jsonPrimitive?.longOrNull ?: 0L
        } else 0L

        val senderObj = msgObj["sender_id"]?.jsonObject
        val senderId = senderObj?.get("user_id")?.jsonPrimitive?.longOrNull
            ?: senderObj?.get("chat_id")?.jsonPrimitive?.longOrNull ?: 0L

        val contentObj = msgObj["content"]?.jsonObject ?: return null
        val contentType = contentObj["@type"]?.jsonPrimitive?.content ?: return null

        return when (contentType) {
            "messageText" -> {
                val text = contentObj["text"]?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""
                Message(
                    id = msgId, chatId = chatId, senderName = if (isOutgoing) "Вы" else "Собеседник",
                    text = text, isOutgoing = isOutgoing, mediaType = MessageMediaType.TEXT,
                    date = date, isSending = isSending, replyToMessageId = replyToId,
                    isEdited = editDate > 0, senderId = senderId, mediaAlbumId = mediaAlbumId
                )
            }
            "messagePhoto" -> {
                val caption = contentObj["caption"]?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""
                val sizes = contentObj["photo"]?.jsonObject?.get("sizes")?.jsonArray
                val bigPhoto = sizes?.lastOrNull()?.jsonObject?.get("photo")?.jsonObject
                val path = bigPhoto?.get("local")?.jsonObject?.get("path")?.jsonPrimitive?.content
                val fileId = bigPhoto?.get("id")?.jsonPrimitive?.intOrNull

                if (path.isNullOrBlank() && fileId != null) {
                    tracker.messagePhotos[fileId] = msgId
                    tdlibClient.send("""{"@type": "downloadFile", "file_id": $fileId, "priority": 1}""")
                }

                Message(
                    id = msgId, chatId = chatId, senderName = if (isOutgoing) "Вы" else "Собеседник",
                    text = caption, photoPath = path, isOutgoing = isOutgoing,
                    mediaType = MessageMediaType.PHOTO, date = date, isSending = isSending,
                    replyToMessageId = replyToId, isEdited = editDate > 0, senderId = senderId, mediaAlbumId = mediaAlbumId
                )
            }
            "messageVideo", "messageVideoNote", "messageAnimation" -> {
                val videoContainer = contentObj[when (contentType) {
                    "messageVideo" -> "video"
                    "messageVideoNote" -> "video_note"
                    else -> "animation" // 💥 ДЛЯ ГИФОК
                }]?.jsonObject

                val thumbObj = videoContainer?.get("thumbnail")?.jsonObject?.get("file")?.jsonObject
                val path = thumbObj?.get("local")?.jsonObject?.get("path")?.jsonPrimitive?.content
                val thumbFileId = thumbObj?.get("id")?.jsonPrimitive?.intOrNull
                val duration = videoContainer?.get("duration")?.jsonPrimitive?.intOrNull ?: 0

                // Для видео файл лежит в "video", для гифок — в "animation"
                val mediaFile = videoContainer?.get(if (contentType == "messageAnimation") "animation" else "video")?.jsonObject
                val videoPath = mediaFile?.get("local")?.jsonObject?.get("path")?.jsonPrimitive?.content
                val videoFileId = mediaFile?.get("id")?.jsonPrimitive?.intOrNull

                // 💥 КАЧАЕМ ПРЕВЬЮ НА МАКСИМАЛЬНОЙ СКОРОСТИ (priority = 32)!
                if (path.isNullOrBlank() && thumbFileId != null) {
                    tracker.messagePhotos[thumbFileId] = msgId
                    tdlibClient.send("""{"@type": "downloadFile", "file_id": $thumbFileId, "priority": 32}""")
                }
                if (videoPath.isNullOrBlank() && videoFileId != null) {
                    tracker.messageFiles[videoFileId] = msgId
                    tdlibClient.send("""{"@type": "downloadFile", "file_id": $videoFileId, "priority": 16}""")
                }

                Message(
                    id = msgId, chatId = chatId, senderName = if (isOutgoing) "Вы" else "Собеседник",
                    text = "", photoPath = path, fileName = videoPath,
                    fileExtraInfo = if (duration > 0) "$duration сек" else "GIF",
                    isOutgoing = isOutgoing,
                    // Все гифки и видео отправляем как VIDEO
                    mediaType = MessageMediaType.VIDEO,
                    date = date, isSending = isSending,
                    replyToMessageId = replyToId, isEdited = editDate > 0, senderId = senderId, mediaAlbumId = mediaAlbumId
                )
            }
           /* "messageVideo", "messageVideoNote" -> {
                val videoObj = contentObj[if (contentType == "messageVideo") "video" else "video_note"]?.jsonObject
                val thumbObj = videoObj?.get("thumbnail")?.jsonObject?.get("file")?.jsonObject
                val path = thumbObj?.get("local")?.jsonObject?.get("path")?.jsonPrimitive?.content
                val thumbFileId = thumbObj?.get("id")?.jsonPrimitive?.intOrNull
                val duration = videoObj?.get("duration")?.jsonPrimitive?.intOrNull ?: 0
                val videoFile = videoObj?.get("video")?.jsonObject
                val videoPath = videoFile?.get("local")?.jsonObject?.get("path")?.jsonPrimitive?.content
                val videoFileId = videoFile?.get("id")?.jsonPrimitive?.intOrNull

                if (path.isNullOrBlank() && thumbFileId != null) {
                    tracker.messagePhotos[thumbFileId] = msgId
                    tdlibClient.send("""{"@type": "downloadFile", "file_id": $thumbFileId, "priority": 1}""")
                }
                if (videoPath.isNullOrBlank() && videoFileId != null) {
                    tracker.messageFiles[videoFileId] = msgId
                    tdlibClient.send("""{"@type": "downloadFile", "file_id": $videoFileId, "priority": 1}""")
                }

                Message(
                    id = msgId,
                    chatId = chatId,
                    senderName = if (isOutgoing) "Вы" else "Собеседник",
                    text = "",
                    photoPath = path,
                    fileName = videoPath,
                    fileExtraInfo = "$duration сек",
                    isOutgoing = isOutgoing,
                    mediaType = MessageMediaType.VIDEO,
                    date = date,
                    isSending = isSending,
                    replyToMessageId = replyToId,
                    isEdited = editDate > 0,
                    senderId = senderId,
                    mediaAlbumId = mediaAlbumId
                )
            }*/
            "messageDocument" -> {
                val docContainer = contentObj["document"]?.jsonObject
                val docFile = docContainer?.get("document")?.jsonObject

                val rawName = docContainer?.get("file_name")?.jsonPrimitive?.content ?: "Файл"
                val filePath = docFile?.get("local")?.jsonObject?.get("path")?.jsonPrimitive?.content
                val fileId = docFile?.get("id")?.jsonPrimitive?.intOrNull
                val sizeBytes = docFile?.get("size")?.jsonPrimitive?.longOrNull ?: 0L

                if (filePath.isNullOrBlank() && fileId != null) {
                    tracker.messageFiles[fileId] = msgId
                    tdlibClient.send("""{"@type": "downloadFile", "file_id": $fileId, "priority": 16}""")
                }

                // 💥 ВАЖНО: Если файл скачан - храним его ПУТЬ в fileName. Если нет - храним ИМЯ.
                val finalPathOrName = if (!filePath.isNullOrBlank()) filePath else rawName

                Message(
                    id = msgId, chatId = chatId, senderName = if (isOutgoing) "Вы" else "Собеседник",
                    text = "",
                    fileName = finalPathOrName, // 💥 ПУТЬ ИЛИ ИМЯ
                    fileExtraInfo = formatFileSize(sizeBytes), // Размер
                    isOutgoing = isOutgoing, mediaType = MessageMediaType.DOCUMENT,
                    date = date, isSending = isSending, replyToMessageId = replyToId,
                    isEdited = editDate > 0, senderId = senderId, mediaAlbumId = mediaAlbumId
                )
            }
            "messageVoiceNote" -> {
                val voiceObj = contentObj["voice_note"]?.jsonObject
                val duration = voiceObj?.get("duration")?.jsonPrimitive?.intOrNull ?: 0
                val voiceFile = voiceObj?.get("voice")?.jsonObject
                val waveform = voiceObj?.get("waveform")?.jsonPrimitive?.content
                val filePath = voiceFile?.get("local")?.jsonObject?.get("path")?.jsonPrimitive?.content
                val fileId = voiceFile?.get("id")?.jsonPrimitive?.intOrNull

                if (filePath.isNullOrBlank() && fileId != null) {
                    tracker.messageFiles[fileId] = msgId
                    tdlibClient.send("""{"@type": "downloadFile", "file_id": $fileId, "priority": 32}""")
                }

                Message(
                    id = msgId, chatId = chatId, senderName = if (isOutgoing) "Вы" else "Собеседник",
                    text = "",
                    fileName = filePath, // 💥 ПУТЬ ГОЛОСОВОЙ
                    fileExtraInfo = formatDuration(duration),
                    isOutgoing = isOutgoing, mediaType = MessageMediaType.VOICE,
                    date = date, isSending = isSending, replyToMessageId = replyToId,
                    isEdited = editDate > 0, senderId = senderId, mediaAlbumId = mediaAlbumId,
                    waveform = waveform
                )
            }
            /*"messageSticker" -> {
                val stickerObj = contentObj["sticker"]?.jsonObject
                val emoji = stickerObj?.get("emoji")?.jsonPrimitive?.content ?: "✨"
                val fileObj = stickerObj?.get("sticker")?.jsonObject
                val stickerPath = fileObj?.get("local")?.jsonObject?.get("path")?.jsonPrimitive?.content
                val fileId = fileObj?.get("id")?.jsonPrimitive?.intOrNull

                if (stickerPath.isNullOrBlank() && fileId != null) {
                    tracker.messagePhotos[fileId] = msgId
                    tdlibClient.send("""{"@type": "downloadFile", "file_id": $fileId, "priority": 1}""")
                }

                Message(
                    id = msgId, chatId = chatId, senderName = if (isOutgoing) "Вы" else "Собеседник",
                    text = "", photoPath = stickerPath, fileExtraInfo = emoji, isOutgoing = isOutgoing,
                    mediaType = MessageMediaType.STICKER, date = date, isSending = isSending,
                    replyToMessageId = replyToId, isEdited = editDate > 0, senderId = senderId, mediaAlbumId = mediaAlbumId
                )
            }*/
            "messageSticker" -> {
                val stickerObj = contentObj["sticker"]?.jsonObject
                val emoji = stickerObj?.get("emoji")?.jsonPrimitive?.content ?: "✨"

                // 💥 1. Превьюшка (статичная .webp)
                val thumbObj = stickerObj?.get("thumbnail")?.jsonObject?.get("file")?.jsonObject
                val thumbFileId = thumbObj?.get("id")?.jsonPrimitive?.intOrNull
                val thumbPath = thumbObj?.get("local")?.jsonObject?.get("path")?.jsonPrimitive?.content

                // 💥 2. Основной файл стикера (.tgs, .webm или .webp)
                val fileObj = stickerObj?.get("sticker")?.jsonObject
                val fileId = fileObj?.get("id")?.jsonPrimitive?.intOrNull
                val stickerPath = fileObj?.get("local")?.jsonObject?.get("path")?.jsonPrimitive?.content

                // Качаем превьюшку
                if (thumbPath.isNullOrBlank() && thumbFileId != null) {
                    tracker.messagePhotos[thumbFileId] = msgId
                    tdlibClient.send("""{"@type": "downloadFile", "file_id": $thumbFileId, "priority": 32}""")
                }

                // Качаем основной файл
                if (stickerPath.isNullOrBlank() && fileId != null) {
                    tracker.messageFiles[fileId] = msgId
                    tdlibClient.send("""{"@type": "downloadFile", "file_id": $fileId, "priority": 16}""")
                }

                Message(
                    id = msgId, chatId = chatId, senderName = if (isOutgoing) "Вы" else "Собеседник",
                    text = "",
                    photoPath = thumbPath ?: stickerPath, // Превьюшка (webp)
                    fileName = stickerPath,              // 💥 САМ ФАЙЛ (.webm / .tgs)
                    fileExtraInfo = emoji, isOutgoing = isOutgoing,
                    mediaType = MessageMediaType.STICKER, date = date, isSending = isSending,
                    replyToMessageId = replyToId, isEdited = editDate > 0, senderId = senderId, mediaAlbumId = mediaAlbumId
                )
            }
            else -> null
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return ""
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> "${(gb * 10).toInt() / 10.0} GB"
        mb >= 1.0 -> "${(mb * 10).toInt() / 10.0} MB"
        kb >= 1.0 -> "${kb.toInt()} KB"
        else -> "$bytes B"
    }
}

private fun formatDuration(seconds: Int): String {
    val min = seconds / 60
    val sec = seconds % 60
    return "$min:${if (sec < 10) "0$sec" else "$sec"}"
}