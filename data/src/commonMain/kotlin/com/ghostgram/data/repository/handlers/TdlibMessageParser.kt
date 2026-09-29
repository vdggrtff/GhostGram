package com.ghostgram.data.repository.handlers

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
            "messageVideo", "messageVideoNote" -> {
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
            }
            "messageDocument" -> {
                val docObj = contentObj["document"]?.jsonObject
                val fileName = docObj?.get("file_name")?.jsonPrimitive?.content ?: "Документ"
                val docFile = docObj?.get("document")?.jsonObject
                val filePath = docFile?.get("local")?.jsonObject?.get("path")?.jsonPrimitive?.content
                val fileId = docFile?.get("id")?.jsonPrimitive?.intOrNull

                if (filePath.isNullOrBlank() && fileId != null) {
                    tracker.messageFiles[fileId] = msgId
                    tdlibClient.send("""{"@type": "downloadFile", "file_id": $fileId, "priority": 1}""")
                }

                Message(
                    id = msgId, chatId = chatId, senderName = if (isOutgoing) "Вы" else "Собеседник",
                    text = "", fileName = fileName, photoPath = filePath, isOutgoing = isOutgoing,
                    mediaType = MessageMediaType.DOCUMENT, date = date, isSending = isSending,
                    replyToMessageId = replyToId, isEdited = editDate > 0, senderId = senderId, mediaAlbumId = mediaAlbumId
                )
            }
            "messageVoiceNote" -> {
                val voiceObj = contentObj["voice_note"]?.jsonObject
                val duration = voiceObj?.get("duration")?.jsonPrimitive?.intOrNull ?: 0
                val voiceFile = voiceObj?.get("voice")?.jsonObject
                val filePath = voiceFile?.get("local")?.jsonObject?.get("path")?.jsonPrimitive?.content
                val fileId = voiceFile?.get("id")?.jsonPrimitive?.intOrNull

                if (filePath.isNullOrBlank() && fileId != null) {
                    tracker.messageFiles[fileId] = msgId
                    tdlibClient.send("""{"@type": "downloadFile", "file_id": $fileId, "priority": 32}""")
                }

                Message(
                    id = msgId, chatId = chatId, senderName = if (isOutgoing) "Вы" else "Собеседник",
                    text = "", fileName = filePath, fileExtraInfo = "$duration сек", isOutgoing = isOutgoing,
                    mediaType = MessageMediaType.VOICE, date = date, isSending = isSending,
                    replyToMessageId = replyToId, isEdited = editDate > 0, senderId = senderId, mediaAlbumId = mediaAlbumId
                )
            }
            "messageSticker" -> {
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
            }
            else -> null
        }
    }
}