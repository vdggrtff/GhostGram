package com.ghostgram.data.repository.handlers

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
    private val tracker: DownloadTracker
) : TdlibUpdateHandler {

    override fun handle(type: String, jsonObject: JsonObject): Boolean {
        when (type) {
            // 💥 1. ПРИШЛО НОВОЕ СООБЩЕНИЕ
            "updateNewMessage" -> {
                val messageObj = jsonObject["message"]?.jsonObject ?: return true
                val chatId = messageObj["chat_id"]?.jsonPrimitive?.longOrNull ?: return true
                val messageId = messageObj["id"]?.jsonPrimitive?.longOrNull ?: return true
                val isOutgoing = messageObj["is_outgoing"]?.jsonPrimitive?.booleanOrNull ?: false

                val contentObj = messageObj["content"]?.jsonObject

                // Парсим сообщение в сущность базы данных
                val entity = parseSingleMessageToEntity(messageId, chatId, isOutgoing, contentObj)

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
                val chatId = messagesArray[0].jsonObject["chat_id"]?.jsonPrimitive?.longOrNull ?: return true

                // Парсим весь массив
                val parsedEntities = messagesArray.mapNotNull { msgElement ->
                    val msgObj = msgElement.jsonObject
                    val msgId = msgObj["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
                    val isOutgoing = msgObj["is_outgoing"]?.jsonPrimitive?.booleanOrNull ?: false
                    val contentObj = msgObj["content"]?.jsonObject

                    parseSingleMessageToEntity(msgId, chatId, isOutgoing, contentObj)
                }.reversed() // Переворачиваем для хронологии

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
                val contentObj = messageObj["content"]?.jsonObject

                val entity = parseSingleMessageToEntity(newId, chatId, true, contentObj)

                repoScope.launch {
                    messageDao.deleteMessage(chatId = chatId, messageId = oldId) // Удалили старое временное
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
                val messageIds = jsonObject["message_ids"]?.jsonArray?.mapNotNull { it.jsonPrimitive.longOrNull } ?: emptyList()

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

    private fun parseSingleMessageToEntity(msgId: Long, chatId: Long, isOutgoing: Boolean, contentObj: JsonObject?): MessageEntity? {
        val contentType = contentObj?.get("@type")?.jsonPrimitive?.content ?: return null
        val senderName = if (isOutgoing) "Вы" else "Собеседник"

        return when (contentType) {
            "messageText" -> {
                val text = contentObj["text"]?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""
                MessageEntity(
                    id = msgId,
                    chatId = chatId,
                    senderName = senderName,
                    text = text,
                    isOutgoing = isOutgoing,
                    mediaType = "TEXT"
                )
            }
            "messagePhoto" -> {
                val caption = contentObj["caption"]?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""
                val photoSizes = contentObj["photo"]?.jsonObject?.get("sizes")?.jsonArray
                val largestPhoto = photoSizes?.lastOrNull()?.jsonObject?.get("photo")?.jsonObject
                val fileId = largestPhoto?.get("id")?.jsonPrimitive?.intOrNull
                val photoPath = largestPhoto?.get("local")?.jsonObject?.get("path")?.jsonPrimitive?.content

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
                    mediaType = "PHOTO"
                )
            }
            "messageDocument" -> {
                val docObj = contentObj["document"]?.jsonObject
                val fileName = docObj?.get("file_name")?.jsonPrimitive?.content ?: "Файл"
                val caption = contentObj["caption"]?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""
                MessageEntity(
                    id = msgId,
                    chatId = chatId,
                    senderName = senderName,
                    text = caption,
                    isOutgoing = isOutgoing,
                    mediaType = "DOCUMENT",
                    fileName = fileName
                )
            }
            "messageVoiceNote" -> {
                val caption = contentObj["caption"]?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""
                MessageEntity(
                    id = msgId,
                    chatId = chatId,
                    senderName = senderName,
                    text = caption,
                    isOutgoing = isOutgoing,
                    mediaType = "VOICE"
                )
            }
            "messageVideo", "messageVideoNote" -> {
                val caption = contentObj["caption"]?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""
                MessageEntity(
                    id = msgId,
                    chatId = chatId,
                    senderName = senderName,
                    text = caption,
                    isOutgoing = isOutgoing,
                    mediaType = if (contentType == "messageVideo") "VIDEO" else "VIDEO_NOTE"
                )
            }
            "messageSticker" -> {
                val emoji = contentObj["sticker"]?.jsonObject?.get("emoji")?.jsonPrimitive?.content ?: "✨"
                MessageEntity(
                    id = msgId,
                    chatId = chatId,
                    senderName = senderName,
                    text = "",
                    isOutgoing = isOutgoing,
                    mediaType = "STICKER",
                    fileExtraInfo = emoji
                )
            }
            else -> {
                MessageEntity(
                    id = msgId,
                    chatId = chatId,
                    senderName = senderName,
                    text = "[Медиа]",
                    isOutgoing = isOutgoing,
                    mediaType = "TEXT"
                )
            }
        }
    }
}

