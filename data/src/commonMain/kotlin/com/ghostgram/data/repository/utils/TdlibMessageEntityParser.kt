package com.ghostgram.data.repository.utils

import com.ghostgram.core.crypto.CryptoLayer
import com.ghostgram.core.crypto.toHex
import com.ghostgram.core.database.entity.MessageEntity
import com.ghostgram.core.tdlib.TelegramFlowClient
import entity.MessageReactionItem
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class TdlibMessageEntityParser(
    private val tdlibClient: TelegramFlowClient,
    private val tracker: DownloadTracker,
    private val cryptoLayer: CryptoLayer,
    private val userCache: UserMemoryCache
) {

    fun parseSingleMessageToEntity(
        msgId: Long,
        chatId: Long,
        isOutgoing: Boolean,
        contentObj: JsonObject?,
        date: Int = 0,
        mediaAlbumId: Long = 0L,
        isLive: Boolean = false,
        isSending: Boolean = false,
        replyToMessageId: Long = 0L,
        isEdited: Boolean = false,
        senderId: Long = 0L,
        reactionsJson: String = "",
    ): MessageEntity? {
        val contentType = contentObj?.get("@type")?.jsonPrimitive?.content ?: return null

        val (realSenderName, realSenderAvatar) = if (isOutgoing) {
            "Вы" to null
        } else if (senderId != 0L) {
            val cached = userCache.getUser(senderId)
            if (cached != null) {
                cached.name to cached.avatarPath
            } else {
                // Если юзера еще нет в памяти — просим TDLib загрузить его
                tdlibClient.send("""{"@type": "getUser", "user_id": $senderId}""")
                "Участник" to null
            }
        } else {
            "Собеседник" to null
        }

        return when (contentType) {
            "messageText" -> {
                var text = contentObj["text"]?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""
                var extraInfo: String? = null

                // 1. E2EE: КТО-ТО ПРЕДЛАГАЕТ НАМ ОБМЕН КЛЮЧАМИ
                if (text.startsWith("👻🔑 ")) {
                    if (!isOutgoing) {
                        val otherPubKeyHex = text.removePrefix("👻🔑 ")
                        cryptoLayer.establishSecret(chatId, otherPubKeyHex)

                        if (isLive) {
                            val myPubKeyHex = cryptoLayer.myKeyPair.second.toHex()
                            tdlibClient.send(
                                """
                                {
                                    "@type": "sendMessage", 
                                    "chat_id": $chatId, 
                                    "input_message_content": {
                                        "@type": "inputMessageText", 
                                        "text": {"@type": "formattedText", "text": "👻🤝 $myPubKeyHex"},
                                        "link_preview_options": {"@type": "linkPreviewOptions", "is_disabled": true}
                                    }
                                }
                                """.trimIndent()
                            )
                        }
                    }
                    text = "🔐 Запрос E2EE отправлен..."
                    extraInfo = "SYSTEM"
                }
                // 2. E2EE: СОБЕСЕДНИК ПОДТВЕРДИЛ ОБМЕН
                else if (text.startsWith("👻🤝 ")) {
                    if (!isOutgoing) {
                        val otherPubKeyHex = text.removePrefix("👻🤝 ")
                        cryptoLayer.establishSecret(chatId, otherPubKeyHex)
                    }
                    text = "✅ Защищенный E2EE канал установлен!"
                    extraInfo = "SYSTEM"
                }
                // 3. E2EE: РАСШИФРОВКА ТЕКСТА
                else if (text.contains("👻 ")) {
                    val decrypted = cryptoLayer.revealAndDecrypt(chatId, text)
                    if (decrypted != null) {
                        text = decrypted
                        extraInfo = "ENCRYPTED"
                    } else {
                        text = "❌ Ошибка дешифровки E2EE\n$text"
                        extraInfo = "SYSTEM"
                    }
                }

                MessageEntity(
                    id = msgId, chatId = chatId, senderName = realSenderName, text = text,
                    isOutgoing = isOutgoing, mediaType = "TEXT", fileExtraInfo = extraInfo,
                    date = date, mediaAlbumId = mediaAlbumId, isSending = isSending,
                    replyToMessageId = replyToMessageId, isEdited = isEdited, senderId = senderId,
                    senderAvatarPath = realSenderAvatar, reactionsJson = reactionsJson
                )
            }

            "messagePhoto" -> {
                val caption = contentObj["caption"]?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""
                val photoSizes = contentObj["photo"]?.jsonObject?.get("sizes")?.jsonArray
                val largestPhoto = photoSizes?.lastOrNull()?.jsonObject?.get("photo")?.jsonObject
                val fileId = largestPhoto?.get("id")?.jsonPrimitive?.intOrNull
                val photoPath = largestPhoto?.get("local")?.jsonObject?.get("path")?.jsonPrimitive?.content

                if (photoPath.isNullOrBlank() && fileId != null && fileId != 0) {
                    tracker.messagePhotos[fileId] = msgId
                    tdlibClient.send("""{"@type": "downloadFile", "file_id": $fileId, "priority": 32, "offset": 0, "limit": 0, "synchronous": false}""")
                }

                MessageEntity(
                    id = msgId, chatId = chatId, senderName = realSenderName, text = caption,
                    isOutgoing = isOutgoing, photoPath = photoPath, mediaType = "PHOTO",
                    date = date, mediaAlbumId = mediaAlbumId, isSending = isSending,
                    replyToMessageId = replyToMessageId, isEdited = isEdited, senderId = senderId,
                    senderAvatarPath = realSenderAvatar, reactionsJson = reactionsJson
                )
            }

            "messageDocument" -> {
                val docObj = contentObj["document"]?.jsonObject
                val fileName = docObj?.get("file_name")?.jsonPrimitive?.content ?: "Файл"
                val caption = contentObj["caption"]?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""
                MessageEntity(
                    id = msgId, chatId = chatId, senderName = realSenderName, text = caption,
                    isOutgoing = isOutgoing, mediaType = "DOCUMENT", fileName = fileName,
                    date = date, mediaAlbumId = mediaAlbumId, isSending = isSending,
                    replyToMessageId = replyToMessageId, isEdited = isEdited, senderId = senderId,
                    senderAvatarPath = realSenderAvatar, reactionsJson = reactionsJson
                )
            }

            "messageVoiceNote" -> {
                val voiceObj = contentObj["voice_note"]?.jsonObject
                val duration = voiceObj?.get("duration")?.jsonPrimitive?.intOrNull ?: 0
                val caption = contentObj["caption"]?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""
                val fileObj = voiceObj?.get("voice")?.jsonObject
                val fileId = fileObj?.get("id")?.jsonPrimitive?.intOrNull
                val filePath = fileObj?.get("local")?.jsonObject?.get("path")?.jsonPrimitive?.content
                val waveform = voiceObj?.get("waveform")?.jsonPrimitive?.content

                if (filePath.isNullOrBlank() && fileId != null && fileId != 0) {
                    tracker.messageFiles[fileId] = msgId
                    tdlibClient.send("""{"@type": "downloadFile", "file_id": $fileId, "priority": 32, "offset": 0, "limit": 0, "synchronous": false}""")
                }
                MessageEntity(
                    id = msgId, chatId = chatId, senderName = realSenderName, text = caption,
                    isOutgoing = isOutgoing, mediaType = "VOICE", fileExtraInfo = formatDuration(duration),
                    fileName = filePath, date = date, mediaAlbumId = mediaAlbumId,
                    isSending = isSending, replyToMessageId = replyToMessageId, isEdited = isEdited,
                    senderId = senderId, senderAvatarPath = realSenderAvatar,
                    waveform = waveform, reactionsJson = reactionsJson
                )
            }

            "messageVideo", "messageVideoNote", "messageAnimation" -> {
                val isNote = contentType == "messageVideoNote"
                val videoContainer = contentObj[when {
                    isNote -> "video_note"
                    contentType == "messageVideo" -> "video"
                    else -> "animation"
                }]?.jsonObject

                val duration = videoContainer?.get("duration")?.jsonPrimitive?.intOrNull ?: 0
                val caption = contentObj["caption"]?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""
                val thumbObj = videoContainer?.get("thumbnail")?.jsonObject?.get("file")?.jsonObject
                val fileId = thumbObj?.get("id")?.jsonPrimitive?.intOrNull
                val photoPath = thumbObj?.get("local")?.jsonObject?.get("path")?.jsonPrimitive?.content
                val targetMediaTypeStr = if (isNote) "VIDEO_NOTE" else "VIDEO"

                if (photoPath.isNullOrBlank() && fileId != null && fileId != 0) {
                    tracker.messagePhotos[fileId] = msgId
                    tdlibClient.send("""{"@type": "downloadFile", "file_id": $fileId, "priority": 32, "offset": 0, "limit": 0, "synchronous": false}""")
                }

                val mediaFile = videoContainer?.get(if (contentType == "messageAnimation") "animation" else if (isNote) "video_note" else "video")?.jsonObject
                val videoFileId = mediaFile?.get("id")?.jsonPrimitive?.intOrNull
                val videoPath = mediaFile?.get("local")?.jsonObject?.get("path")?.jsonPrimitive?.content

                if (videoPath.isNullOrBlank() && videoFileId != null && videoFileId != 0) {
                    tracker.messageFiles[videoFileId] = msgId
                    tdlibClient.send("""{"@type": "downloadFile", "file_id": $videoFileId, "priority": 16, "offset": 0, "limit": 0, "synchronous": false}""")
                }

                MessageEntity(
                    id = msgId, chatId = chatId, senderName = realSenderName, text = caption,
                    isOutgoing = isOutgoing, mediaType = targetMediaTypeStr,
                    fileExtraInfo = if (duration > 0) formatDuration(duration) else "GIF",
                    photoPath = photoPath, date = date, mediaAlbumId = mediaAlbumId,
                    fileName = videoPath, isSending = isSending, replyToMessageId = replyToMessageId,
                    isEdited = isEdited, senderId = senderId, senderAvatarPath = realSenderAvatar,
                    reactionsJson = reactionsJson
                )
            }

            "messageSticker" -> {
                val stickerObj = contentObj["sticker"]?.jsonObject
                val emoji = stickerObj?.get("emoji")?.jsonPrimitive?.content ?: "✨"

                val thumbObj = stickerObj?.get("thumbnail")?.jsonObject?.get("file")?.jsonObject
                val thumbFileId = thumbObj?.get("id")?.jsonPrimitive?.intOrNull
                val thumbPath = thumbObj?.get("local")?.jsonObject?.get("path")?.jsonPrimitive?.content

                val fileObj = stickerObj?.get("sticker")?.jsonObject
                val fileId = fileObj?.get("id")?.jsonPrimitive?.intOrNull
                val stickerPath = fileObj?.get("local")?.jsonObject?.get("path")?.jsonPrimitive?.content

                if (thumbPath.isNullOrBlank() && thumbFileId != null && thumbFileId != 0) {
                    tracker.messagePhotos[thumbFileId] = msgId
                    tdlibClient.send("""{"@type": "downloadFile", "file_id": $thumbFileId, "priority": 32}""")
                }

                if (stickerPath.isNullOrBlank() && fileId != null && fileId != 0) {
                    tracker.messageFiles[fileId] = msgId
                    tdlibClient.send("""{"@type": "downloadFile", "file_id": $fileId, "priority": 16}""")
                }

                MessageEntity(
                    id = msgId, chatId = chatId, senderName = realSenderName, text = "",
                    isOutgoing = isOutgoing, mediaType = "STICKER", fileExtraInfo = emoji,
                    photoPath = thumbPath ?: stickerPath, fileName = stickerPath, date = date,
                    isSending = isSending, replyToMessageId = replyToMessageId, isEdited = isEdited,
                    senderId = senderId, senderAvatarPath = realSenderAvatar,
                    reactionsJson = reactionsJson
                )
            }

            else -> {
                MessageEntity(
                    id = msgId, chatId = chatId, senderName = realSenderName, text = "[Медиа]",
                    isOutgoing = isOutgoing, mediaType = "TEXT", date = date, mediaAlbumId = mediaAlbumId,
                    isSending = isSending, replyToMessageId = replyToMessageId, isEdited = isEdited,
                    senderId = senderId, senderAvatarPath = realSenderAvatar, reactionsJson = reactionsJson
                )
            }
        }
    }

    fun extractReactionsJson(baseObj: JsonObject): String {
        return try {
            val interactionInfo = baseObj["interaction_info"]?.jsonObject ?: return ""
            val messageReactionsObj = interactionInfo["reactions"]?.jsonObject ?: return ""
            val reactionsArr = messageReactionsObj["reactions"]?.jsonArray ?: return ""

            val items = reactionsArr.mapNotNull { elem ->
                val rObj = elem.jsonObject
                val typeObj = rObj["type"]?.jsonObject
                val emoji = typeObj?.get("emoji")?.jsonPrimitive?.content ?: return@mapNotNull null
                val count = rObj["total_count"]?.jsonPrimitive?.intOrNull ?: 1
                val isChosen = rObj["is_chosen"]?.jsonPrimitive?.booleanOrNull ?: false
                MessageReactionItem(emoji, count, isChosen)
            }

            if (items.isNotEmpty()) Json.encodeToString(items) else ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun formatDuration(seconds: Int): String {
        val min = seconds / 60
        val sec = seconds % 60
        return "$min:${if (sec < 10) "0$sec" else "$sec"}"
    }
}