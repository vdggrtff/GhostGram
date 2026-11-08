package com.ghostgram.data.repository.handlers

import com.ghostgram.core.crypto.CryptoLayer
import com.ghostgram.core.crypto.toHex
import com.ghostgram.core.database.dao.MessageDao
import com.ghostgram.core.database.entity.MessageEntity
import com.ghostgram.core.tdlib.TelegramFlowClient
import com.ghostgram.data.repository.utils.ChatMemoryCache
import com.ghostgram.data.repository.utils.DownloadTracker
import com.ghostgram.data.repository.utils.TdlibMessageEntityParser
import com.ghostgram.data.repository.utils.UserMemoryCache
import entity.MessageMediaType
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
    private val tdlibClient: TelegramFlowClient,
    private val chatCache: ChatMemoryCache,
    private val tracker: DownloadTracker,
    private val cryptoLayer: CryptoLayer,
    private val userCache: UserMemoryCache
) : TdlibUpdateHandler {

    // 💥 Вынесенный чистый парсер сущностей
    private val entityParser = TdlibMessageEntityParser(tdlibClient, tracker, cryptoLayer, userCache)

    override fun handle(type: String, jsonObject: JsonObject): Boolean {
        return when (type) {
            "updateNewMessage" -> handleNewMessage(jsonObject)
            "messages" -> handleMessagesHistory(jsonObject)
            "updateMessageSendSucceeded" -> handleMessageSendSucceeded(jsonObject)
            "updateDeleteMessages" -> handleDeleteMessages(jsonObject)
            "updateMessageContent" -> handleMessageContent(jsonObject)
            "updateMessageEdited" -> handleMessageEdited(jsonObject)
            "user", "updateUser" -> handleUserUpdate(type, jsonObject)
            "updateFile" -> handleFileUpdate(jsonObject)
            "updateMessageInteractionInfo" -> handleMessageInteractionInfo(jsonObject)
            else -> false
        }
    }

    // 💥 1. НОВОЕ СООБЩЕНИЕ
    private fun handleNewMessage(jsonObject: JsonObject): Boolean {
        val messageObj = jsonObject["message"]?.jsonObject ?: return true
        val chatId = messageObj["chat_id"]?.jsonPrimitive?.longOrNull ?: return true
        val messageId = messageObj["id"]?.jsonPrimitive?.longOrNull ?: return true
        val isOutgoing = messageObj["is_outgoing"]?.jsonPrimitive?.booleanOrNull ?: false
        val date = messageObj["date"]?.jsonPrimitive?.intOrNull ?: 0
        val mediaAlbumId = messageObj["media_album_id"]?.jsonPrimitive?.longOrNull ?: 0L
        val isSending = messageObj["sending_state"] != null
        val replyToObj = messageObj["reply_to"]?.jsonObject
        val replyToMessageId = if (replyToObj?.get("@type")?.jsonPrimitive?.content == "messageReplyToMessage") {
            replyToObj["message_id"]?.jsonPrimitive?.longOrNull ?: 0L
        } else 0L
        val editDate = messageObj["edit_date"]?.jsonPrimitive?.intOrNull ?: 0
        val senderObj = messageObj["sender_id"]?.jsonObject
        val senderId = senderObj?.get("user_id")?.jsonPrimitive?.longOrNull
            ?: senderObj?.get("chat_id")?.jsonPrimitive?.longOrNull ?: 0L
        val reactionsJson = entityParser.extractReactionsJson(messageObj)
        val contentObj = messageObj["content"]?.jsonObject

        val entity = entityParser.parseSingleMessageToEntity(
            messageId, chatId, isOutgoing, contentObj, date, mediaAlbumId,
            isLive = true, isSending = isSending, replyToMessageId = replyToMessageId,
            isEdited = editDate > 0, senderId = senderId, reactionsJson = reactionsJson
        )

        if (entity != null) {
            repoScope.launch { messageDao.insertMessage(entity) }
        }
        return true
    }

    // 💥 2. ИСТОРИЯ ЧАТА
    private fun handleMessagesHistory(jsonObject: JsonObject): Boolean {
        val messagesArray = jsonObject["messages"]?.jsonArray ?: return true
        if (messagesArray.isEmpty()) return true
        val chatId = messagesArray[0].jsonObject["chat_id"]?.jsonPrimitive?.longOrNull ?: return true

        val parsedEntities = messagesArray.reversed().mapNotNull { msgElement ->
            val msgObj = msgElement.jsonObject
            val msgId = msgObj["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
            val isOutgoing = msgObj["is_outgoing"]?.jsonPrimitive?.booleanOrNull ?: false
            val isSending = msgObj["sending_state"] != null
            val replyToObj = msgObj["reply_to"]?.jsonObject
            val replyToMessageId = if (replyToObj?.get("@type")?.jsonPrimitive?.content == "messageReplyToMessage") {
                replyToObj["message_id"]?.jsonPrimitive?.longOrNull ?: 0L
            } else 0L
            val contentObj = msgObj["content"]?.jsonObject
            val date = msgObj["date"]?.jsonPrimitive?.intOrNull ?: 0
            val mediaAlbumId = msgObj["media_album_id"]?.jsonPrimitive?.longOrNull ?: 0L
            val editDate = msgObj["edit_date"]?.jsonPrimitive?.intOrNull ?: 0
            val senderObj = msgObj["sender_id"]?.jsonObject
            val senderId = senderObj?.get("user_id")?.jsonPrimitive?.longOrNull
                ?: senderObj?.get("chat_id")?.jsonPrimitive?.longOrNull ?: 0L
            val reactionsJson = entityParser.extractReactionsJson(msgObj)

            entityParser.parseSingleMessageToEntity(
                msgId, chatId, isOutgoing, contentObj, date, mediaAlbumId,
                isLive = false, isSending = isSending, replyToMessageId = replyToMessageId,
                isEdited = editDate > 0, senderId = senderId, reactionsJson = reactionsJson
            )
        }

        repoScope.launch {
            messageDao.insertMessages(parsedEntities)
            val lastReadId = chatCache.lastReadOutboxMap[chatId] ?: 0L
            if (lastReadId > 0) messageDao.markOutboxAsRead(chatId, lastReadId)
        }
        return true
    }

    // 💥 3. УСПЕШНАЯ ОТПРАВКА
    private fun handleMessageSendSucceeded(jsonObject: JsonObject): Boolean {
        val oldId = jsonObject["old_message_id"]?.jsonPrimitive?.longOrNull ?: return true
        val messageObj = jsonObject["message"]?.jsonObject ?: return true
        val chatId = messageObj["chat_id"]?.jsonPrimitive?.longOrNull ?: return true
        val newId = messageObj["id"]?.jsonPrimitive?.longOrNull ?: return true
        val date = messageObj["date"]?.jsonPrimitive?.intOrNull ?: 0
        val mediaAlbumId = messageObj["media_album_id"]?.jsonPrimitive?.longOrNull ?: 0L
        val contentObj = messageObj["content"]?.jsonObject
        val replyToObj = messageObj["reply_to"]?.jsonObject
        val replyToMessageId = if (replyToObj?.get("@type")?.jsonPrimitive?.content == "messageReplyToMessage") {
            replyToObj["message_id"]?.jsonPrimitive?.longOrNull ?: 0L
        } else 0L

        val entity = entityParser.parseSingleMessageToEntity(
            newId, chatId, true, contentObj, replyToMessageId = replyToMessageId,
            date = date, mediaAlbumId = mediaAlbumId, isLive = false, isSending = false
        )

        repoScope.launch {
            messageDao.deleteMessage(chatId = chatId, messageId = oldId)
            if (entity != null) messageDao.insertMessage(entity)
        }
        return true
    }

    // 💥 4. УДАЛЕНИЕ (ANTI-REVOKE)
    private fun handleDeleteMessages(jsonObject: JsonObject): Boolean {
        val chatId = jsonObject["chat_id"]?.jsonPrimitive?.longOrNull ?: return true
        val messageIds = jsonObject["message_ids"]?.jsonArray?.mapNotNull { it.jsonPrimitive.longOrNull } ?: emptyList()

        repoScope.launch {
            messageIds.forEach { msgId -> messageDao.markAsDeleted(chatId = chatId, messageId = msgId) }
        }
        return true
    }

    // 💥 5. РЕДАКТИРОВАНИЕ ТЕКСТА (E2EE)
    private fun handleMessageContent(jsonObject: JsonObject): Boolean {
        val chatId = jsonObject["chat_id"]?.jsonPrimitive?.longOrNull ?: return true
        val msgId = jsonObject["message_id"]?.jsonPrimitive?.longOrNull ?: return true
        val newContent = jsonObject["new_content"]?.jsonObject ?: return true

        if (newContent["@type"]?.jsonPrimitive?.content == "messageText") {
            var text = newContent["text"]?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""
            var extraInfo: String? = null

            if (text.contains("👻 ")) {
                val decrypted = cryptoLayer.revealAndDecrypt(chatId, text)
                if (decrypted != null) {
                    text = decrypted
                    extraInfo = "ENCRYPTED"
                }
            }

            repoScope.launch { messageDao.updateMessageText(msgId, text, extraInfo) }
        }
        return true
    }

    private fun handleMessageEdited(jsonObject: JsonObject): Boolean {
        val msgId = jsonObject["message_id"]?.jsonPrimitive?.longOrNull ?: return true
        repoScope.launch { messageDao.markMessageAsEdited(msgId) }
        return true
    }

    // 💥 6. ОБНОВЛЕНИЕ ДАННЫХ ЮЗЕРА (НИК И АВАТАРКА)
    private fun handleUserUpdate(type: String, jsonObject: JsonObject): Boolean {
        if (jsonObject["@extra"]?.jsonPrimitive?.content == "get_me_avatar") return false
        val userObj = if (type == "updateUser") jsonObject["user"]?.jsonObject else jsonObject
        val userId = userObj?.get("id")?.jsonPrimitive?.longOrNull ?: return true
        val firstName = userObj["first_name"]?.jsonPrimitive?.content ?: ""
        val lastName = userObj["last_name"]?.jsonPrimitive?.content ?: ""
        val fullName = "$firstName $lastName".trim().ifBlank { "Участник" }

        val photoObj = userObj["profile_photo"]?.jsonObject
        val smallPhoto = photoObj?.get("small")?.jsonObject
        val fileId = smallPhoto?.get("id")?.jsonPrimitive?.intOrNull
        val avatarPath = smallPhoto?.get("local")?.jsonObject?.get("path")?.jsonPrimitive?.content

        // Запоминаем в памяти
        userCache.putUser(userId, fullName, avatarPath)

        // Если фото еще качается — ставим на слежение в трекер
        if (avatarPath.isNullOrBlank() && fileId != null && fileId != 0) {
            tracker.chatAvatars[fileId] = userId
            tdlibClient.send("""{"@type": "downloadFile", "file_id": $fileId, "priority": 16, "offset": 0, "limit": 0, "synchronous": false}""")
        }

        // Обновляем базу Room
        repoScope.launch {
            messageDao.updateSenderInfo(userId, fullName, avatarPath)
        }
        return true
    }

    // 💥 7. ЗАВЕРШЕНИЕ СКАЧИВАНИЯ АВАТАРКИ (ВОТ ЧТО ВОЗВРАЩАЕТ АВАТАРКИ В ГРУППЫ!)
    private fun handleFileUpdate(jsonObject: JsonObject): Boolean {
        val fileObj = jsonObject["file"]?.jsonObject ?: return false
        val fileId = fileObj["id"]?.jsonPrimitive?.intOrNull ?: return false
        val localObj = fileObj["local"]?.jsonObject ?: return false
        val isCompleted = localObj["is_downloading_completed"]?.jsonPrimitive?.booleanOrNull ?: false
        val path = localObj["path"]?.jsonPrimitive?.content

        if (isCompleted && !path.isNullOrBlank()) {
            // 💥 АВАТАРКА ЧАТА ИЛИ УЧАСТНИКА:
            tracker.chatAvatars.remove(fileId)?.let { targetId ->
                // 1. Обновляем аватарку в чатах (ChatMemoryCache) -> для списка чатов и TopBar!
                chatCache.updateChat(targetId) { it.copy(avatarPath = path) }

                // 2. Обновляем аватарку юзера (UserMemoryCache) -> для пузырей сообщений в группах!
                userCache.updateAvatar(targetId, path)
                val userName = userCache.getUser(targetId)?.name ?: "Участник"
                repoScope.launch {
                    messageDao.updateSenderInfo(targetId, userName, path)
                }
            }
            // Медиафайлы сообщений
            tracker.messagePhotos.remove(fileId)?.let { msgId ->
                repoScope.launch { messageDao.updateMessagePhoto(msgId, path) }
            }
            tracker.messageFiles.remove(fileId)?.let { msgId ->
                repoScope.launch { messageDao.updateMessagePhoto(msgId, path) }
            }
        }
        return false
    }

    private fun handleMessageInteractionInfo(jsonObject: JsonObject): Boolean {
        val msgId = jsonObject["message_id"]?.jsonPrimitive?.longOrNull ?: return true
        val jsonStr = entityParser.extractReactionsJson(jsonObject)
        repoScope.launch { messageDao.updateMessageReactions(msgId, jsonStr) }
        return true
    }
}
