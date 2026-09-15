package com.ghostgram.data.repository.handlers

import com.ghostgram.core.database.dao.MessageDao
import com.ghostgram.core.tdlib.TelegramFlowClient
import entity.Chat
import entity.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

class ChatUpdateHandler(
    private val chatsMap: MutableStateFlow<Map<Long, Chat>>,
    private val lastReadOutboxMap: MutableMap<Long, Long>,
    private val tdlibClient: TelegramFlowClient,
    private val messageDao: MessageDao,
    private val repoScope: CoroutineScope,
    private val tracker: DownloadTracker
) : TdlibUpdateHandler {

    override fun handle(type: String, jsonObject: JsonObject): Boolean {
        when (type) {

            // 💥 1. ЗАГРУЗКА ИЛИ ОБНОВЛЕНИЕ ЧАТА
            "updateNewChat" -> {
                val chatObj = jsonObject["chat"]?.jsonObject ?: return true
                val id = chatObj["id"]?.jsonPrimitive?.longOrNull ?: return true
                val title = chatObj["title"]?.jsonPrimitive?.content ?: "Без названия"
                val unreadCount = chatObj["unread_count"]?.jsonPrimitive?.intOrNull ?: 0
                val lastMsgObj = chatObj["last_message"]?.jsonObject
                val previewText = parsePreviewText(lastMsgObj?.get("content")?.jsonObject)
                val dateUnix = lastMsgObj?.get("date")?.jsonPrimitive?.intOrNull ?: 0
                val lastReadOutbox = chatObj["last_read_outbox_message_id"]?.jsonPrimitive?.longOrNull ?: 0L
                lastReadOutboxMap[id] = lastReadOutbox
                val photoObj = chatObj["photo"]?.jsonObject
                val smallPhoto = photoObj?.get("small")?.jsonObject
                val fileId = smallPhoto?.get("id")?.jsonPrimitive?.intOrNull
                val avatarPath = smallPhoto?.get("local")?.jsonObject?.get("path")?.jsonPrimitive?.content
                val positions = chatObj["positions"]?.jsonArray
                val isJoined = positions != null && positions.isNotEmpty()

                // Если фото нет на диске, но есть ID — качаем!
                if (avatarPath.isNullOrBlank() && fileId != null && fileId != 0) {
                    tracker.chatAvatars[fileId] = id // 💥 Записали в трекер!
                    tdlibClient.send("""{"@type": "downloadFile", "file_id": $fileId, "priority": 1, "offset": 0, "limit": 0, "synchronous": false}""")
                }

                val lastMessage = if (previewText != null) {
                   Message(id = 0, chatId = id, senderName = "", text = previewText, date = dateUnix)
                } else null

                val chat = Chat(
                    id = id,
                    title = title,
                    unreadCount = unreadCount,
                    lastMessage = lastMessage,
                    avatarPath = if (!avatarPath.isNullOrBlank()) avatarPath else null,
                    isJoined = isJoined
                )

                chatsMap.update { it + (id to chat) }
                return true
            }
            "updateChatPosition" -> {
                val chatId = jsonObject["chat_id"]?.jsonPrimitive?.longOrNull ?: return true
                val positionObj = jsonObject["position"]?.jsonObject ?: return true
                val listType = positionObj["list"]?.jsonObject?.get("@type")?.jsonPrimitive?.content

                // Нас интересует только главный список чатов
                if (listType == "chatListMain") {
                    val order = positionObj["order"]?.jsonPrimitive?.longOrNull ?: 0L

                    chatsMap.update { current ->
                        val chat = current[chatId]
                        // Обновляем позицию чата
                        if (chat != null) current + (chatId to chat.copy(order = order)) else current
                    }
                }
                return true
            }
            "updateChatLastMessage" -> {
                val chatId = jsonObject["chat_id"]?.jsonPrimitive?.longOrNull ?: return true
                val lastMsgObj = jsonObject["last_message"]?.jsonObject
                val dateUnix = lastMsgObj?.get("date")?.jsonPrimitive?.intOrNull ?: 0

                val previewText = parsePreviewText(lastMsgObj?.get("content")?.jsonObject)

                chatsMap.update { current ->
                    val chat = current[chatId]
                    if (chat != null) {
                        val lastMessage = if (previewText != null) {
                            Message(id = 0, chatId = chatId, senderName = "", text = previewText, date = dateUnix)
                        } else null

                        current + (chatId to chat.copy(lastMessage = lastMessage))
                    } else {
                        current
                    }
                }
                return true
            }

            // 💥 2. МЫ ПРОЧИТАЛИ СООБЩЕНИЯ (Сбрасываем счетчик)
            "updateChatReadInbox" -> {
                val chatId = jsonObject["chat_id"]?.jsonPrimitive?.longOrNull ?: return true
                val unreadCount = jsonObject["unread_count"]?.jsonPrimitive?.intOrNull ?: 0

                chatsMap.update { current ->
                    val chat = current[chatId]
                    if (chat != null) current + (chatId to chat.copy(unreadCount = unreadCount)) else current
                }
                return true
            }

            // 💥 3. СОБЕСЕДНИК ПРОЧИТАЛ НАШИ СООБЩЕНИЯ (Ставим ✓✓)
            "updateChatReadOutbox" -> {
                val chatId = jsonObject["chat_id"]?.jsonPrimitive?.longOrNull ?: return true
                val lastReadId = jsonObject["last_read_outbox_message_id"]?.jsonPrimitive?.longOrNull ?: return true

                lastReadOutboxMap[chatId] = lastReadId
                repoScope.launch {
                    messageDao.markOutboxAsRead(chatId, lastReadId)
                }
                return true
            }
        }

        return false
    }
    private fun parsePreviewText(contentObj: JsonObject?): String? {
        val msgType = contentObj?.get("@type")?.jsonPrimitive?.content
        return when (msgType) {
            "messageText" -> {
                val text = contentObj["text"]?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""
                when {
                    text.startsWith("👻🔑 ") -> "🔐 Запрос на секретный чат"
                    text.startsWith("👻🤝 ") -> "✅ Секретный чат установлен"
                    text.startsWith("👻 ") -> "🔒 Секретное сообщение"
                    else -> text
                }
            }
            "messagePhoto" -> "📷 Фотография"
            "messageVideo", "messageVideoNote" -> "🎥 Видео"
            "messageDocument" -> "📄 Документ"
            "messageVoiceNote" -> "🎤 Голосовое сообщение"
            "messageSticker" -> "✨ Стикер"
            else -> null
        }
    }
}

