package com.ghostgram.data.repository.handlers

import com.ghostgram.core.crypto.CryptoLayer
import com.ghostgram.core.crypto.toHex
import com.ghostgram.core.database.dao.MessageDao
import com.ghostgram.core.database.entity.MessageEntity
import com.ghostgram.core.tdlib.TelegramFlowClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

class MessageUpdateHandler(
    private val messageDao: MessageDao,
    private val repoScope: CoroutineScope,
    private val tdlibClient: TelegramFlowClient,          // Нужен для скачивания картинок
    private val lastReadOutboxMap: MutableMap<Long, Long>,
    private val tracker: DownloadTracker,
    private val cryptoLayer: CryptoLayer,
) : TdlibUpdateHandler {

    override fun handle(type: String, jsonObject: JsonObject): Boolean {
        when (type) {
            // 💥 1. ПРИШЛО НОВОЕ СООБЩЕНИЕ
            "updateNewMessage" -> {
                val messageObj = jsonObject["message"]?.jsonObject ?: return true
                val chatId = messageObj["chat_id"]?.jsonPrimitive?.longOrNull ?: return true
                val messageId = messageObj["id"]?.jsonPrimitive?.longOrNull ?: return true
                val isOutgoing = messageObj["is_outgoing"]?.jsonPrimitive?.booleanOrNull ?: false
                val date = messageObj["date"]?.jsonPrimitive?.intOrNull ?: 0
                val mediaAlbumId = messageObj["media_album_id"]?.jsonPrimitive?.longOrNull ?: 0L

                val contentObj = messageObj["content"]?.jsonObject

                // Парсим сообщение в сущность базы данных
                val entity =
                    parseSingleMessageToEntity(
                        messageId,
                        chatId,
                        isOutgoing,
                        contentObj,
                        date,
                        mediaAlbumId,
                        isLive = true
                    )

                if (entity != null) {
                    repoScope.launch {
                        messageDao.insertMessage(entity)
                    }
                }
                return true
            }
            // 💥 2. ПРИЛЕТЕЛА ИСТОРИЯ ЧАТА (ПАЧКА СООБЩЕНИЙ)
            "messages" -> {
                val messagesArray = jsonObject["messages"]?.jsonArray ?: return true
                if (messagesArray.isEmpty()) return true
                val chatId =
                    messagesArray[0].jsonObject["chat_id"]?.jsonPrimitive?.longOrNull ?: return true

                // Парсим весь массив
                val parsedEntities = messagesArray.reversed().mapNotNull { msgElement ->
                    val msgObj = msgElement.jsonObject
                    val msgId = msgObj["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
                    val isOutgoing = msgObj["is_outgoing"]?.jsonPrimitive?.booleanOrNull ?: false
                    val contentObj = msgObj["content"]?.jsonObject

                    val date = msgObj["date"]?.jsonPrimitive?.intOrNull ?: 0
                    // 💥 ДОСТАЕМ АЛЬБОМ ИЗ ИСТОРИИ!
                    val mediaAlbumId = msgObj["media_album_id"]?.jsonPrimitive?.longOrNull ?: 0L

                    parseSingleMessageToEntity(
                        msgId,
                        chatId,
                        isOutgoing,
                        contentObj,
                        date,
                        mediaAlbumId,
                        isLive = false
                    )
                }

                repoScope.launch {
                    // Сохраняем пачку в базу
                    messageDao.insertMessages(parsedEntities)

                    // Восстанавливаем синие галочки ✓✓
                    val lastReadId = lastReadOutboxMap[chatId] ?: 0L
                    if (lastReadId > 0) {
                        messageDao.markOutboxAsRead(chatId, lastReadId)
                    }
                }
                return true
            }
            // 💥 3. УБИВАЕМ ФАНТОМА (Сообщение успешно доставлено на сервер)
            "updateMessageSendSucceeded" -> {
                val oldId = jsonObject["old_message_id"]?.jsonPrimitive?.longOrNull ?: return true
                val messageObj = jsonObject["message"]?.jsonObject ?: return true
                val chatId = messageObj["chat_id"]?.jsonPrimitive?.longOrNull ?: return true
                val newId = messageObj["id"]?.jsonPrimitive?.longOrNull ?: return true
                val date = messageObj["date"]?.jsonPrimitive?.intOrNull ?: 0
                val mediaAlbumId = messageObj["media_album_id"]?.jsonPrimitive?.longOrNull ?: 0L
                val contentObj = messageObj["content"]?.jsonObject

                val entity = parseSingleMessageToEntity(
                    newId, chatId, true, contentObj, date = date,
                    mediaAlbumId = mediaAlbumId,
                    isLive = false
                )

                repoScope.launch {
                    messageDao.deleteMessage(
                        chatId = chatId,
                        messageId = oldId
                    ) // Удалили старое временное
                    if (entity != null) {
                        messageDao.insertMessage(entity) // Записали новое настоящее
                    }
                }
                return true
            }
            // 💥 4. СООБЩЕНИЕ ОТРЕДАКТИРОВАНО (Фиксим текст в базе)
            "updateMessageContent" -> {
                val chatId = jsonObject["chat_id"]?.jsonPrimitive?.longOrNull ?: return true
                val msgId = jsonObject["message_id"]?.jsonPrimitive?.longOrNull ?: return true
                val newContent = jsonObject["new_content"]?.jsonObject ?: return true

                val newText = newContent["text"]?.jsonObject?.get("text")?.jsonPrimitive?.content

                if (newText != null) {
                    repoScope.launch {
                        messageDao.updateMessageText(chatId, msgId, newText)
                    }
                }
                return true
            }
            // 💥 5. ANTI-REVOKE (Собеседник удалил сообщение)
            "updateDeleteMessages" -> {
                val chatId = jsonObject["chat_id"]?.jsonPrimitive?.longOrNull ?: return true
                val messageIds =
                    jsonObject["message_ids"]?.jsonArray?.mapNotNull { it.jsonPrimitive.longOrNull }
                        ?: emptyList()

                repoScope.launch {
                    messageIds.forEach { msgId ->
                        messageDao.markAsDeleted(chatId = chatId, messageId = msgId)
                    }
                }
                return true
            }
        }
        return false // Если тип другой — мы его не трогаем
    }

    private fun parseSingleMessageToEntity(
        msgId: Long,
        chatId: Long,
        isOutgoing: Boolean,
        contentObj: JsonObject?,
        date: Int = 0,
        mediaAlbumId: Long = 0L,
        isLive: Boolean = false,
    ): MessageEntity? {
        val contentType = contentObj?.get("@type")?.jsonPrimitive?.content ?: return null
        val senderName = if (isOutgoing) "Вы" else "Собеседник"

        return when (contentType) {
            "messageText" -> {
                var text = contentObj["text"]?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""
                var extraInfo: String? = null

                // 💥 1. КТО-ТО ПРЕДЛАГАЕТ НАМ ОБМЕН КЛЮЧАМИ
                if (text.startsWith("👻🔑 ")) {
                    // 💥 ТОЛЬКО ДЛЯ ЧУЖИХ СООБЩЕНИЙ СОХРАНЯЕМ СЕКРЕТ! (чтобы не сломать ключ о самого себя)
                    if (!isOutgoing) {
                        val otherPubKeyHex = text.removePrefix("👻🔑 ")
                        cryptoLayer.establishSecret(chatId, otherPubKeyHex)

                        // Отвечаем ТОЛЬКО если это новое сообщение (isLive == true), а не из истории!
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
                // 💥 2. СОБЕСЕДНИК ПОДТВЕРДИЛ ОБМЕН
                else if (text.startsWith("👻🤝 ")) {
                    // 💥 ТОЛЬКО ДЛЯ ЧУЖИХ СООБЩЕНИЙ!
                    if (!isOutgoing) {
                        val otherPubKeyHex = text.removePrefix("👻🤝 ")
                        cryptoLayer.establishSecret(chatId, otherPubKeyHex)
                    }
                    text = "✅ Защищенный E2EE канал установлен!"
                    extraInfo = "SYSTEM"
                }
                // 💥 3. РАСШИФРОВКА ТЕКСТА
                else if (text.contains("👻 ")) {
                    val decrypted = cryptoLayer.revealAndDecrypt(chatId, text)
                    if (decrypted != null) {
                        text = decrypted
                        extraInfo = "ENCRYPTED" // Ставим метку для зеленого замка в UI
                    } else {
                        text = "❌ Ошибка дешифровки E2EE\n$text"
                        extraInfo = "SYSTEM"
                    }
                }

                MessageEntity(
                    id = msgId,
                    chatId = chatId,
                    senderName = senderName,
                    text = text,
                    isOutgoing = isOutgoing,
                    mediaType = "TEXT",
                    fileExtraInfo = extraInfo,
                    date = date,
                    mediaAlbumId = mediaAlbumId // 💥 Альбомы спасены!
                )
            }

            "messagePhoto" -> {
                val caption =
                    contentObj["caption"]?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""
                val photoSizes = contentObj["photo"]?.jsonObject?.get("sizes")?.jsonArray
                val largestPhoto = photoSizes?.lastOrNull()?.jsonObject?.get("photo")?.jsonObject
                val fileId = largestPhoto?.get("id")?.jsonPrimitive?.intOrNull
                val photoPath =
                    largestPhoto?.get("local")?.jsonObject?.get("path")?.jsonPrimitive?.content

                // Просим TDLib скачать фотку, если её нет на диске
                if (photoPath.isNullOrBlank() && fileId != null && fileId != 0) {
                    tracker.messagePhotos[fileId] = msgId // 💥 Записали в трекер!
                    tdlibClient.send("""{"@type": "downloadFile", "file_id": $fileId, "priority": 1, "offset": 0, "limit": 0, "synchronous": false}""")
                }

                MessageEntity(
                    id = msgId,
                    chatId = chatId,
                    senderName = senderName,
                    text = caption,
                    isOutgoing = isOutgoing,
                    photoPath = photoPath,
                    mediaType = "PHOTO",
                    date = date,
                    mediaAlbumId = mediaAlbumId
                )
            }

            "messageDocument" -> {
                val docObj = contentObj["document"]?.jsonObject
                val fileName = docObj?.get("file_name")?.jsonPrimitive?.content ?: "Файл"
                val caption =
                    contentObj["caption"]?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""
                MessageEntity(
                    id = msgId,
                    chatId = chatId,
                    senderName = senderName,
                    text = caption,
                    isOutgoing = isOutgoing,
                    mediaType = "DOCUMENT",
                    fileName = fileName,
                    date = date,
                    mediaAlbumId = mediaAlbumId
                )
            }

            "messageVoiceNote" -> {
                val caption =
                    contentObj["caption"]?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""
                MessageEntity(
                    id = msgId,
                    chatId = chatId,
                    senderName = senderName,
                    text = caption,
                    isOutgoing = isOutgoing,
                    mediaType = "VOICE",
                    date = date,
                    mediaAlbumId = mediaAlbumId
                )
            }

            "messageVideo", "messageVideoNote" -> {
                val videoObj =
                    contentObj[if (contentType == "messageVideo") "video" else "video_note"]?.jsonObject
                val duration = videoObj?.get("duration")?.jsonPrimitive?.intOrNull ?: 0
                val caption =
                    contentObj["caption"]?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""

                // 💥 ДОСТАЕМ КАРТИНКУ-ПРЕВЬЮШКУ ВИДЕО!
                val thumbObj = videoObj?.get("thumbnail")?.jsonObject?.get("file")?.jsonObject
                val fileId = thumbObj?.get("id")?.jsonPrimitive?.intOrNull
                val photoPath =
                    thumbObj?.get("local")?.jsonObject?.get("path")?.jsonPrimitive?.content

                if (photoPath.isNullOrBlank() && fileId != null && fileId != 0) {
                    tracker.messagePhotos[fileId] = msgId
                    tdlibClient.send("""{"@type": "downloadFile", "file_id": $fileId, "priority": 1, "offset": 0, "limit": 0, "synchronous": false}""")
                }

                MessageEntity(
                    id = msgId,
                    chatId = chatId,
                    senderName = senderName,
                    text = caption,
                    isOutgoing = isOutgoing,
                    mediaType = if (contentType == "messageVideo") "VIDEO" else "VIDEO_NOTE",
                    fileExtraInfo = formatDuration(duration),
                    photoPath = photoPath, // 💥 Сохраняем путь к превьюшке!
                    date = date,
                    mediaAlbumId = mediaAlbumId
                )
            }
            "messageSticker" -> {
                val stickerObj = contentObj["sticker"]?.jsonObject
                val emoji = stickerObj?.get("emoji")?.jsonPrimitive?.content ?: "✨"

                // 💥 КАЧАЕМ ПОЛНОЦЕННЫЙ СТИКЕР (не thumbnail!)
                val fileObj = stickerObj?.get("sticker")?.jsonObject
                val fileId = fileObj?.get("id")?.jsonPrimitive?.intOrNull
                val stickerPath = fileObj?.get("local")?.jsonObject?.get("path")?.jsonPrimitive?.content

                if (stickerPath.isNullOrBlank() && fileId != null && fileId != 0) {
                    tracker.messagePhotos[fileId] = msgId
                    tdlibClient.send("""{"@type": "downloadFile", "file_id": $fileId, "priority": 1, "offset": 0, "limit": 0, "synchronous": false}""")
                }

                MessageEntity(
                    id = msgId, chatId = chatId, senderName = senderName, text = "",
                    isOutgoing = isOutgoing, mediaType = "STICKER", fileExtraInfo = emoji,
                    photoPath = stickerPath, date = date // (твои параметры)
                )
            }

            else -> {
                MessageEntity(
                    id = msgId,
                    chatId = chatId,
                    senderName = senderName,
                    text = "[Медиа]",
                    isOutgoing = isOutgoing,
                    mediaType = "TEXT",
                    date = date,
                    mediaAlbumId = mediaAlbumId
                )
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val min = seconds / 60
    val sec = seconds % 60
    // Если секунд меньше 10, добавляем нолик спереди (чтобы было 1:05, а не 1:5)
    return "$min:${if (sec < 10) "0$sec" else "$sec"}"
}

