package com.ghostgram.data.repository

import com.ghostgram.core.database.dao.MessageDao
import com.ghostgram.core.database.entity.MessageEntity
import com.ghostgram.core.tdlib.TelegramFlowClient
import entity.Chat
import entity.Message
import entity.MessageMediaType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

class ChatRepositoryImpl(
    private val tdlibClient: TelegramFlowClient,
    private val messageDao: MessageDao,
) : ChatRepository {

    private val jsonParser = Json { ignoreUnknownKeys = true }

    // Потокобезопасный кэш чатов в памяти: Map<ChatId, Chat>
    private val _chatsMap = MutableStateFlow<Map<Long, Chat>>(emptyMap())

    private val _messagesState = MutableStateFlow<Map<Long, List<Message>>>(emptyMap())

    // Карта для отслеживания скачиваемых файлов: fileId -> (chatId, messageId?)
    private val downloadingAvatars = mutableMapOf<Int, Long>() // fileId -> chatId
    private val downloadingPhotos =
        mutableMapOf<Int, Pair<Long, Long>>() // fileId -> (chatId, messageId)
    private val repoScope = CoroutineScope(Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    private var myUserId: Long = 0L
    private val _myAvatarPath = MutableStateFlow<String?>(null)
    private var myAvatarFileId: Int? = null

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

            val extra = jsonObject["@extra"]?.jsonPrimitive?.content

            if (extra == "get_me_avatar" && type == "user") {
                val firstName = jsonObject["first_name"]?.jsonPrimitive?.content ?: "Я"
                println("📸 TDLib Профиль: Меня зовут $firstName") // Смотрим в Logcat!

                val photoObj = jsonObject["profile_photo"]?.jsonObject
                val smallPhoto = photoObj?.get("small")?.jsonObject
                val fileId = smallPhoto?.get("id")?.jsonPrimitive?.intOrNull
                val path = smallPhoto?.get("local")?.jsonObject?.get("path")?.jsonPrimitive?.content

                println("📸 TDLib Профиль: fileId=$fileId, path=$path")

                if (!path.isNullOrBlank()) {
                    _myAvatarPath.value = path
                } else if (fileId != null && fileId != 0) {
                    myAvatarFileId = fileId
                    downloadFile(fileId)
                } else {
                    // Аватарки в профиле нет! Отдаем спец-флаг для инициалов
                    _myAvatarPath.value = "INITIALS:$firstName"
                }
                return
            }

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
                    var avatarPath =
                        smallPhoto?.get("local")?.jsonObject?.get("path")?.jsonPrimitive?.content

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
                    val isOutgoing =
                        messageObj["is_outgoing"]?.jsonPrimitive?.booleanOrNull ?: false

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
                    val chatId =
                        messagesArray[0].jsonObject["chat_id"]?.jsonPrimitive?.longOrNull ?: return

                    val parsedMessages = messagesArray.mapNotNull { msgElement ->
                        val msgObj = msgElement.jsonObject
                        val msgId =
                            msgObj["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
                        val isOutgoing =
                            msgObj["is_outgoing"]?.jsonPrimitive?.booleanOrNull ?: false
                        val contentObj = msgObj["content"]?.jsonObject
                        parseSingleMessage(msgId, chatId, isOutgoing, contentObj)
                    }.reversed()

                    _messagesState.update { current -> current + (chatId to parsedMessages) }

                    repoScope.launch {
                        messageDao.insertMessages(parsedMessages.map {
                            MessageEntity(
                                id = it.id,
                                chatId = it.chatId,
                                senderName = it.senderName,
                                text = it.text,
                                isOutgoing = it.isOutgoing,
                                photoPath = it.photoPath
                            )
                        })
                    }
                }

                // 💥 4. ФАЙЛ СКАЧАЛСЯ! Прилетел готовый путь на диске
                "updateFile" -> {
                    val fileObj = jsonObject["file"]?.jsonObject ?: return
                    val fileId = fileObj["id"]?.jsonPrimitive?.intOrNull ?: return
                    val localObj = fileObj["local"]?.jsonObject ?: return
                    val isCompleted =
                        localObj["is_downloading_completed"]?.jsonPrimitive?.booleanOrNull ?: false
                    val path = localObj["path"]?.jsonPrimitive?.content ?: ""

                    if (isCompleted && path.isNotBlank()) {

                        if (fileId == myAvatarFileId) {
                            _myAvatarPath.value = path
                        }

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
                    val messageIds =
                        jsonObject["message_ids"]?.jsonArray?.mapNotNull { it.jsonPrimitive.longOrNull }
                            ?: emptyList()

                    _messagesState.update { current ->
                        val list = current[chatId] ?: emptyList()
                        current + (chatId to list.map { msg ->
                            if (msg.id in messageIds) msg.copy(isDeletedLocally = true) else msg
                        })
                    }

                    repoScope.launch {
                        messageIds.forEach { msgId ->
                            messageDao.markAsDeleted(
                                chatId = chatId,
                                messageId = msgId
                            )
                        }
                    }
                }

                "updateOption" -> {
                    if (jsonObject["name"]?.jsonPrimitive?.content == "my_id") {
                        val id =
                            jsonObject["value"]?.jsonObject?.get("value")?.jsonPrimitive?.content?.toLongOrNull()
                                ?: jsonObject["value"]?.jsonObject?.get("value")?.jsonPrimitive?.longOrNull
                        if (id != null) myUserId = id
                    }
                }

                "updateUser", "user" -> {
                    val userObj =
                        if (type == "updateUser") jsonObject["user"]?.jsonObject else jsonObject
                    val id = userObj?.get("id")?.jsonPrimitive?.longOrNull ?: return

                    // Если это ТВОЙ профиль — забираем аватарку!
                    if (id == myUserId && myUserId != 0L) {
                        val photoObj = userObj["profile_photo"]?.jsonObject
                        val smallPhoto = photoObj?.get("small")?.jsonObject
                        val fileId = smallPhoto?.get("id")?.jsonPrimitive?.intOrNull
                        val path =
                            smallPhoto?.get("local")?.jsonObject?.get("path")?.jsonPrimitive?.content

                        if (!path.isNullOrBlank()) {
                            _myAvatarPath.value = path
                        } else if (fileId != null && fileId != 0) {
                            myAvatarFileId = fileId
                            downloadFile(fileId) // 💥 Запускаем скачивание!
                        }
                    }
                }
            }
        } catch (e: Exception) {
        }
    }

    private fun parseSingleMessage(
        msgId: Long,
        chatId: Long,
        isOutgoing: Boolean,
        contentObj: JsonObject?,
    ): Message? {
        val contentType = contentObj?.get("@type")?.jsonPrimitive?.content ?: return null

        return when (contentType) {
            // Обычный текст
            "messageText" -> {
                val text = contentObj["text"]?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""
                Message(
                    id = msgId,
                    chatId = chatId,
                    senderName = if (isOutgoing) "Вы" else "Собеседник",
                    text = text,
                    isOutgoing = isOutgoing,
                    mediaType = MessageMediaType.TEXT
                )
            }

            // Фотография
            "messagePhoto" -> {
                val caption =
                    contentObj["caption"]?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""
                val photoSizes = contentObj["photo"]?.jsonObject?.get("sizes")?.jsonArray
                val largestPhoto = photoSizes?.lastOrNull()?.jsonObject?.get("photo")?.jsonObject
                val fileId = largestPhoto?.get("id")?.jsonPrimitive?.intOrNull
                var photoPath =
                    largestPhoto?.get("local")?.jsonObject?.get("path")?.jsonPrimitive?.content

                if (photoPath.isNullOrBlank() && fileId != null && fileId != 0) {
                    downloadingPhotos[fileId] = chatId to msgId
                    downloadFile(fileId)
                }

                Message(
                    id = msgId,
                    chatId = chatId,
                    senderName = if (isOutgoing) "Вы" else "Собеседник",
                    text = caption,
                    isOutgoing = isOutgoing,
                    photoPath = photoPath,
                    mediaType = MessageMediaType.PHOTO
                )
            }

            // 📄 Документ / Файл
            "messageDocument" -> {
                val docObj = contentObj["document"]?.jsonObject
                val fileName = docObj?.get("file_name")?.jsonPrimitive?.content ?: "Файл"
                val sizeBytes =
                    docObj?.get("document")?.jsonObject?.get("size")?.jsonPrimitive?.longOrNull
                        ?: 0L
                val caption =
                    contentObj["caption"]?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""

                Message(
                    id = msgId,
                    chatId = chatId,
                    senderName = if (isOutgoing) "Вы" else "Собеседник",
                    text = caption,
                    isOutgoing = isOutgoing,
                    mediaType = MessageMediaType.DOCUMENT,
                    fileName = fileName,
                    fileExtraInfo = formatFileSize(sizeBytes)
                )
            }

            // 🎤 Голосовое сообщение
            "messageVoiceNote" -> {
                val voiceObj = contentObj["voice_note"]?.jsonObject
                val duration = voiceObj?.get("duration")?.jsonPrimitive?.intOrNull ?: 0
                val caption =
                    contentObj["caption"]?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""

                Message(
                    id = msgId,
                    chatId = chatId,
                    senderName = if (isOutgoing) "Вы" else "Собеседник",
                    text = caption,
                    isOutgoing = isOutgoing,
                    mediaType = MessageMediaType.VOICE,
                    fileExtraInfo = formatDuration(duration)
                )
            }

            // 🎥 Видео
            "messageVideo" -> {
                val videoObj = contentObj["video"]?.jsonObject
                val duration = videoObj?.get("duration")?.jsonPrimitive?.intOrNull ?: 0
                val fileName = videoObj?.get("file_name")?.jsonPrimitive?.content ?: "Видео"
                val caption =
                    contentObj["caption"]?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""

                Message(
                    id = msgId,
                    chatId = chatId,
                    senderName = if (isOutgoing) "Вы" else "Собеседник",
                    text = caption,
                    isOutgoing = isOutgoing,
                    mediaType = MessageMediaType.VIDEO,
                    fileName = fileName,
                    fileExtraInfo = formatDuration(duration)
                )
            }

            // ⭕️ Кружочек (Video Note)
            "messageVideoNote" -> {
                val videoNoteObj = contentObj["video_note"]?.jsonObject
                val duration = videoNoteObj?.get("duration")?.jsonPrimitive?.intOrNull ?: 0

                Message(
                    id = msgId,
                    chatId = chatId,
                    senderName = if (isOutgoing) "Вы" else "Собеседник",
                    text = "",
                    isOutgoing = isOutgoing,
                    mediaType = MessageMediaType.VIDEO_NOTE,
                    fileExtraInfo = formatDuration(duration)
                )
            }

            // 🎭 Стикер
            "messageSticker" -> {
                val stickerObj = contentObj["sticker"]?.jsonObject
                val emoji = stickerObj?.get("emoji")?.jsonPrimitive?.content ?: "✨"

                Message(
                    id = msgId,
                    chatId = chatId,
                    senderName = if (isOutgoing) "Вы" else "Собеседник",
                    text = "",
                    isOutgoing = isOutgoing,
                    mediaType = MessageMediaType.STICKER,
                    fileExtraInfo = emoji
                )
            }

            else -> {
                Message(
                    id = msgId,
                    chatId = chatId,
                    senderName = if (isOutgoing) "Вы" else "Собеседник",
                    text = "[Медиа]",
                    isOutgoing = isOutgoing
                )
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

    override fun observeChat(chatId: Long): Flow<Chat?> {
        return _chatsMap.map { it[chatId] }
    }

    override fun observeMyAvatar(): Flow<String?> {
        tdlibClient.send("""{"@type": "getMe", "@extra": "get_me_avatar"}""")
        return _myAvatarPath.asStateFlow()
    }

    override fun observeMessages(chatId: Long): Flow<List<Message>> {
        // 1. Говорим Telegram, что мы смотрим в чат
        tdlibClient.send("""{"@type": "openChat", "chat_id": $chatId}""")

        // 💥 2. АГРЕССИВНАЯ АВТОДОКАЧКА ДЛЯ МЕДЛЕННЫХ ЭМУЛЯТОРОВ
        repoScope.launch {
            repeat(4) { // Пингуем ядро 4 раза
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
                kotlinx.coroutines.delay(1200) // Ждем 1.2 секунды между попытками
            }
        }

        // 3. Отдаем поток из Room
        return messageDao.observeMessages(chatId)
            .map { entities ->
                entities.map { entity ->
                    Message(
                        id = entity.id, chatId = entity.chatId,
                        senderName = entity.senderName, text = entity.text,
                        isOutgoing = entity.isOutgoing, isDeletedLocally = entity.isDeletedLocally,
                        photoPath = entity.photoPath
                        // mediaType, fileName и т.д. (если ты их добавил)
                    )
                }
            }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun sendMessage(chatId: Long, text: String) {
        tdlibClient.send("""{"@type": "sendMessage", "chat_id": $chatId, "input_message_content": {"@type": "inputMessageText", "text": {"@type": "formattedText", "text": "$text"}}}""")
    }

    override suspend fun getChatHistory(chatId: Long, limit: Int): String {
        val messages = _messagesState.value[chatId] ?: emptyList()
        return messages.takeLast(limit).joinToString("\n") { "${it.senderName}: ${it.text}" }
    }

    // Утилиты форматирования размера и длительности
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            bytes >= 1024 -> "${bytes / 1024} KB"
            else -> "$bytes B"
        }
    }

    private fun formatDuration(seconds: Int): String {
        val min = seconds / 60
        val sec = seconds % 60
        return "$min:${if (sec < 10) "0$sec" else "$sec"}"
    }
}
