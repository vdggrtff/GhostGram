package com.ghostgram.data.repository

import com.ghostgram.core.database.dao.MessageDao
import com.ghostgram.core.database.entity.MessageEntity
import com.ghostgram.core.tdlib.TelegramFlowClient
import entity.Chat
import entity.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import repository.ChatRepository
import kotlin.to

class ChatRepositoryImpl(
    private val tdlibClient: TelegramFlowClient,
    private val messageDao: MessageDao
) : ChatRepository {

    private val jsonParser = Json { ignoreUnknownKeys = true }

    // Потокобезопасный кэш чатов в памяти: Map<ChatId, Chat>
    private val _chatsMap = MutableStateFlow<Map<Long, Chat>>(emptyMap())

    private val _messagesState = MutableStateFlow<Map<Long, List<Message>>>(emptyMap())

    // Карта для отслеживания скачиваемых файлов: fileId -> (chatId, messageId?)
    private val downloadingAvatars = mutableMapOf<Int, Long>() // fileId -> chatId
    private val downloadingPhotos = mutableMapOf<Int, Pair<Long, Long>>() // fileId -> (chatId, messageId)

    // Кэш сообщений: Map<ChatId, List<Message>>
    private val _messagesMap = MutableStateFlow<Map<Long, List<Message>>>(emptyMap())
    private val repoScope = CoroutineScope(Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    private val pendingCloudSync = mutableSetOf<Long>()

    init {
        // Запускаем фонового слушателя апдейтов от TDLib
        tdlibClient.updates
            .onEach { rawJson -> parseTdlibUpdate(rawJson) }
            .launchIn(repoScope)
    }

    private fun parseTdlibUpdate(rawJson: String) {
        try {
            val jsonObject = jsonParser.parseToJsonElement(rawJson).jsonObject
            val type = jsonObject["@type"]?.jsonPrimitive?.content ?: return

            when (type) {
                // 1. ЧАТЫ И АВАТАРКИ
                "updateNewChat" -> {
                    val chatObj = jsonObject["chat"]?.jsonObject ?: return
                    val id = chatObj["id"]?.jsonPrimitive?.longOrNull ?: return
                    val title = chatObj["title"]?.jsonPrimitive?.content ?: "Без названия"
                    val unreadCount = chatObj["unread_count"]?.jsonPrimitive?.intOrNull ?: 0

                    // Достаем аватарку
                    val photoObj = chatObj["photo"]?.jsonObject
                    val smallPhoto = photoObj?.get("small")?.jsonObject
                    val fileId = smallPhoto?.get("id")?.jsonPrimitive?.intOrNull
                    var avatarPath = smallPhoto?.get("local")?.jsonObject?.get("path")?.jsonPrimitive?.content

                    if (avatarPath.isNullOrBlank() && fileId != null && fileId != 0) {
                        downloadingAvatars[fileId] = id
                        downloadFile(fileId) // 💥 Запускаем скачивание аватарки
                    }

                    val chat = Chat(
                        id = id, title = title, unreadCount = unreadCount, lastMessage = null,
                        avatarPath = if (!avatarPath.isNullOrBlank()) avatarPath else null
                    )
                    _chatsMap.update { it + (id to chat) }
                }

                // 2. СООБЩЕНИЯ С ФОТОГРАФИЯМИ
                "updateNewMessage" -> {
                    val messageObj = jsonObject["message"]?.jsonObject ?: return
                    val chatId = messageObj["chat_id"]?.jsonPrimitive?.longOrNull ?: return
                    val messageId = messageObj["id"]?.jsonPrimitive?.longOrNull ?: return
                    val isOutgoing = messageObj["is_outgoing"]?.jsonPrimitive?.booleanOrNull ?: false

                    val contentObj = messageObj["content"]?.jsonObject
                    val parsedMsg = parseSingleMessage(messageId, chatId, isOutgoing, contentObj)

                    if (parsedMsg != null) {
                        _messagesState.update { current ->
                            val list = current[chatId] ?: emptyList()
                            current + (chatId to (list + parsedMsg))
                        }

                        repoScope.launch {
                            messageDao.insertMessage(
                                MessageEntity(
                                    id = messageId, chatId = chatId,
                                    senderName = parsedMsg.senderName, text = parsedMsg.text,
                                    isOutgoing = isOutgoing, photoPath = parsedMsg.photoPath
                                )
                            )
                        }
                    }
                }

                // 3. ИСТОРИЯ ЧАТА
                "messages" -> {
                    val messagesArray = jsonObject["messages"]?.jsonArray ?: return
                    if (messagesArray.isEmpty()) return
                    val chatId = messagesArray[0].jsonObject["chat_id"]?.jsonPrimitive?.longOrNull ?: return

                    val parsedMessages = messagesArray.mapNotNull { msgElement ->
                        val msgObj = msgElement.jsonObject
                        val msgId = msgObj["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
                        val isOutgoing = msgObj["is_outgoing"]?.jsonPrimitive?.booleanOrNull ?: false
                        val contentObj = msgObj["content"]?.jsonObject
                        parseSingleMessage(msgId, chatId, isOutgoing, contentObj)
                    }.reversed()

                    _messagesState.update { current -> current + (chatId to parsedMessages) }

                    repoScope.launch {
                        messageDao.insertMessages(
                            parsedMessages.map {
                                MessageEntity(id = it.id, chatId = it.chatId, senderName = it.senderName, text = it.text, isOutgoing = it.isOutgoing, photoPath = it.photoPath)
                            }
                        )
                    }

                    if (chatId in pendingCloudSync) {
                        pendingCloudSync.remove(chatId)
                        repoScope.launch {
                            kotlinx.coroutines.delay(400)
                            tdlibClient.send("""{"@type": "getChatHistory", "chat_id": $chatId, "from_message_id": 0, "offset": 0, "limit": 50, "only_local": false}""")
                        }
                    }
                }

                // 💥 4. ФАЙЛ СКАЧАЛСЯ! Прилетел готовый путь на диске
                "updateFile" -> {
                    val fileObj = jsonObject["file"]?.jsonObject ?: return
                    val fileId = fileObj["id"]?.jsonPrimitive?.intOrNull ?: return
                    val localObj = fileObj["local"]?.jsonObject ?: return
                    val isCompleted = localObj["is_downloading_completed"]?.jsonPrimitive?.booleanOrNull ?: false
                    val path = localObj["path"]?.jsonPrimitive?.content ?: ""

                    if (isCompleted && path.isNotBlank()) {
                        // Аватарка скачалась
                        downloadingAvatars.remove(fileId)?.let { chatId ->
                            _chatsMap.update { current ->
                                val chat = current[chatId]
                                if (chat != null) current + (chatId to chat.copy(avatarPath = path)) else current
                            }
                        }

                        // Фотография в сообщении скачалась
                        downloadingPhotos.remove(fileId)?.let { (chatId, messageId) ->
                            _messagesState.update { current ->
                                val list = current[chatId] ?: emptyList()
                                current + (chatId to list.map { msg ->
                                    if (msg.id == messageId) msg.copy(photoPath = path) else msg
                                })
                            }
                        }
                    }
                }

                // Anti-Revoke
                "updateDeleteMessages" -> {
                    val chatId = jsonObject["chat_id"]?.jsonPrimitive?.longOrNull ?: return
                    val messageIds = jsonObject["message_ids"]?.jsonArray?.mapNotNull { it.jsonPrimitive.longOrNull } ?: emptyList()

                    _messagesState.update { current ->
                        val list = current[chatId] ?: emptyList()
                        current + (chatId to list.map { msg ->
                            if (msg.id in messageIds) msg.copy(isDeletedLocally = true) else msg
                        })
                    }

                    repoScope.launch {
                        messageIds.forEach { msgId -> messageDao.markAsDeleted(chatId = chatId, messageId = msgId) }
                    }
                }
            }
        } catch (e: Exception) { }
    }

    private fun parseSingleMessage(msgId: Long, chatId: Long, isOutgoing: Boolean, contentObj: JsonObject?): Message? {
        val contentType = contentObj?.get("@type")?.jsonPrimitive?.content ?: return null

        return when (contentType) {
            "messageText" -> {
                val text = contentObj["text"]?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""
                Message(id = msgId, chatId = chatId, senderName = if (isOutgoing) "Вы" else "Собеседник", text = text, isOutgoing = isOutgoing)
            }
            "messagePhoto" -> {
                val caption = contentObj["caption"]?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""
                val photoSizes = contentObj["photo"]?.jsonObject?.get("sizes")?.jsonArray
                val largestPhoto = photoSizes?.lastOrNull()?.jsonObject?.get("photo")?.jsonObject
                val fileId = largestPhoto?.get("id")?.jsonPrimitive?.intOrNull
                var photoPath = largestPhoto?.get("local")?.jsonObject?.get("path")?.jsonPrimitive?.content

                if (photoPath.isNullOrBlank() && fileId != null && fileId != 0) {
                    downloadingPhotos[fileId] = chatId to msgId
                    downloadFile(fileId) // 💥 Запускаем загрузку фото
                }

                Message(
                    id = msgId, chatId = chatId, senderName = if (isOutgoing) "Вы" else "Собеседник",
                    text = caption, isOutgoing = isOutgoing, photoPath = if (!photoPath.isNullOrBlank()) photoPath else null
                )
            }
            else -> {
                Message(id = msgId, chatId = chatId, senderName = if (isOutgoing) "Вы" else "Собеседник", text = "[Медиа]", isOutgoing = isOutgoing)
            }
        }
    }

    private fun downloadFile(fileId: Int) {
        tdlibClient.send("""{"@type": "downloadFile", "file_id": $fileId, "priority": 1, "offset": 0, "limit": 0, "synchronous": false}""")
    }


    override fun observeChats(): Flow<List<Chat>> {
        tdlibClient.send("""{"@type": "loadChats", "chat_list": {"@type": "chatListMain"}, "limit": 30}""")
        return _chatsMap.map { it.values.toList() }
    }

    override fun observeMessages(chatId: Long): Flow<List<Message>> {
        pendingCloudSync.add(chatId)
        tdlibClient.send("""{"@type": "openChat", "chat_id": $chatId}""")
        tdlibClient.send("""{"@type": "getChatHistory", "chat_id": $chatId, "from_message_id": 0, "offset": 0, "limit": 50, "only_local": false}""")
        return _messagesState.map { it[chatId] ?: emptyList() }
        /*repoScope.launch {
            messageDao.observeMessages(chatId).firstOrNull()?.let { entities ->
                if (entities.isNotEmpty()) {
                    _messagesState.update { current ->
                        current + (chatId to entities.map { entity ->
                            Message(
                                id = entity.id, chatId = entity.chatId,
                                senderName = entity.senderName, text = entity.text,
                                isOutgoing = entity.isOutgoing, isDeletedLocally = entity.isDeletedLocally
                            )
                        })
                    }
                }
            }
        }

        tdlibClient.send("""
            {
                "@type": "getChatHistory",
                "chat_id": $chatId,
                "from_message_id": 0,
                "offset": 0,
                "limit": 50,
                "only_local": false
            }
        """.trimIndent())

        // UI слушает реактивный поток из памяти
        return _messagesState.map { it[chatId] ?: emptyList() }*/
    }

    override suspend fun sendMessage(chatId: Long, text: String) {
        tdlibClient.send("""{"@type": "sendMessage", "chat_id": $chatId, "input_message_content": {"@type": "inputMessageText", "text": {"@type": "formattedText", "text": "$text"}}}""")
    }

    override suspend fun getChatHistory(chatId: Long, limit: Int): String {
        val messages = _messagesState.value[chatId] ?: emptyList()
        return messages.takeLast(limit).joinToString("\n") { "${it.senderName}: ${it.text}" }
    }
}
