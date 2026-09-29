package com.ghostgram.data.repository

import com.ghostgram.core.crypto.CryptoLayer
import com.ghostgram.core.crypto.toHex
import com.ghostgram.core.database.dao.MessageDao
import com.ghostgram.core.tdlib.TelegramFlowClient
import com.ghostgram.data.repository.handlers.ChatUpdateHandler
import com.ghostgram.data.repository.handlers.DownloadTracker
import com.ghostgram.data.repository.handlers.MessageUpdateHandler
import com.ghostgram.data.repository.handlers.ProfileAndFileHandler
import com.ghostgram.data.repository.handlers.SearchUpdateHandler
import com.ghostgram.data.repository.handlers.StickerUpdateHandler
import com.ghostgram.data.repository.handlers.TdlibUpdateHandler
import entity.Chat
import entity.ChatFullProfile
import entity.Message
import entity.MessageMediaType
import entity.MyProfile
import entity.PublicChat
import entity.TelegramSticker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.serializer
import okio.FileSystem
import okio.Path
import okio.SYSTEM
import repository.ChatRepository
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Clock.System
import kotlin.time.Duration.Companion.milliseconds

class ChatRepositoryImpl(
    private val tdlibClient: TelegramFlowClient,
    private val messageDao: MessageDao,
    private val cryptoLayer: CryptoLayer,
) : ChatRepository {

    private val jsonParser = Json { ignoreUnknownKeys = true }

    // Потокобезопасный кэш чатов в памяти: Map<ChatId, Chat>
    private val _chatsMap = MutableStateFlow<Map<Long, Chat>>(emptyMap())
    private val repoScope = CoroutineScope(Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    private val _isGhostModeEnabled = MutableStateFlow(true)

    private val lastReadOutboxMap = mutableMapOf<Long, Long>()

    override fun observeGhostMode(): Flow<Boolean> = _isGhostModeEnabled.asStateFlow()
    private val downloadTracker = DownloadTracker()

    private val _myProfile = MutableStateFlow(MyProfile())

    private val _searchResults = MutableStateFlow<List<PublicChat>>(emptyList())

    private val _messageSearchResults = MutableStateFlow<List<Chat>>(emptyList())

    private val _recentStickers = MutableStateFlow<List<TelegramSticker>>(emptyList())

    private val handlers: List<TdlibUpdateHandler> = listOf(
        SearchUpdateHandler(_searchResults, _messageSearchResults, _chatsMap),
        MessageUpdateHandler(messageDao, repoScope, tdlibClient, lastReadOutboxMap, downloadTracker, cryptoLayer),
        ChatUpdateHandler(_chatsMap, lastReadOutboxMap, tdlibClient, messageDao, repoScope, downloadTracker),
        ProfileAndFileHandler(_myProfile, _chatsMap, tdlibClient, messageDao, repoScope, downloadTracker),
        StickerUpdateHandler(_recentStickers, tdlibClient, downloadTracker),
    )


    init {
        tdlibClient.updates
            .onEach { rawJson ->
                try {
                    val jsonObject = jsonParser.parseToJsonElement(rawJson).jsonObject
                    val type = jsonObject["@type"]?.jsonPrimitive?.content ?: return@onEach

                    // Просто перебираем хэндлеры
                    for (handler in handlers) {
                        if (handler.handle(type, jsonObject)) break
                    }
                } catch (e: Exception) {}
            }
            .launchIn(repoScope)
    }

    override fun observeChats(): Flow<List<Chat>> {
        tdlibClient.send("""{"@type": "getMe"}""")
        tdlibClient.send("""{"@type": "loadChats", "chat_list": {"@type": "chatListMain"}, "limit": 30}""")

        return _chatsMap.map { map ->
            map.values
                .filter { it.order > 0L } // ВЫКИДЫВАЕМ ВЕСЬ МУСОР ИЗ КЭША!
                .sortedByDescending { it.order } // Сортируем (свежие чаты сверху!)
        }
    }

    override fun observeChat(chatId: Long): Flow<Chat?> {
        return _chatsMap.map { it[chatId] }
    }

    override suspend fun loadMoreMessages(chatId: Long, fromMessageId: Long) {
        // Просим еще 50 старых сообщений, начиная от fromMessageId
        tdlibClient.send("""
            {
                "@type": "getChatHistory",
                "chat_id": $chatId,
                "from_message_id": $fromMessageId,
                "offset": 0,
                "limit": 50,
                "only_local": false
            }
        """.trimIndent())
    }

    override fun observeMessages(chatId: Long): Flow<List<Message>> {
        // 1. Говорим Telegram, что мы смотрим в этот чат
        tdlibClient.send("""{"@type": "openChat", "chat_id": $chatId}""")

        // 2. Агрессивная автодокачка для медленных сетей и эмуляторов
        repoScope.launch {
            repeat(4) {
                tdlibClient.send(
                    """
                    {
                        "@type": "getChatHistory",
                        "chat_id": $chatId,
                        "from_message_id": 0,
                        "offset": 0,
                        "limit": 50,
                        "only_local": false
                    }
                """.trimIndent()
                )
                kotlinx.coroutines.delay(1200)
            }
        }

        // 3. ЕДИНЫЙ ПОТОК ИЗ ROOM С АВТОМАТИЧЕСКОЙ ПРОВЕРКОЙ GHOST MODE
        return messageDao.observeMessages(chatId)
            .onEach { entities ->
                // Как только из базы прилетают сообщения — проверяем Ghost Mode и шлем прочтение
                if (entities.isNotEmpty()) {
                    markChatAsRead(chatId, entities.map { it.id })
                }
            }
            .map { entities ->
                entities.map { entity ->
                    Message(
                        id = entity.id,
                        chatId = entity.chatId,
                        senderName = entity.senderName,
                        text = entity.text,
                        isOutgoing = entity.isOutgoing,
                        isDeletedLocally = entity.isDeletedLocally,
                        photoPath = entity.photoPath,
                        mediaType = runCatching { MessageMediaType.valueOf(entity.mediaType) }.getOrDefault(
                            MessageMediaType.TEXT
                        ),
                        fileName = entity.fileName,
                        fileExtraInfo = entity.fileExtraInfo,
                        isRead = entity.isRead,
                        date = entity.date,
                        mediaAlbumId = entity.mediaAlbumId,
                        isSending = entity.isSending,
                        replyToMessageId = entity.replyToMessageId,
                        isEdited = entity.isEdited,
                        senderId = entity.senderId,
                        senderAvatarPath = entity.senderAvatarPath
                    )
                }
            }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun sendMessage(chatId: Long, text: String) {
        tdlibClient.send("""{"@type": "sendMessage", "chat_id": $chatId, "input_message_content": {"@type": "inputMessageText", "text": {"@type": "formattedText", "text": "$text"}}}""")
    }

    override suspend fun getChatHistory(chatId: Long, limit: Int): String {
        // ТЕПЕРЬ МЫ БЕРЕМ ИСТОРИЮ ПРЯМО ИЗ БАЗЫ ДАННЫХ ROOM!
        val entities = messageDao.observeMessages(chatId).firstOrNull() ?: emptyList()

        if (entities.isEmpty()) return ""

        // Склеиваем последние N сообщений в текст для Gemini
        return entities
            .takeLast(limit)
            .filter { it.fileExtraInfo != "ENCRYPTED" }
            .joinToString("\n") { entity ->
                "${entity.senderName}: ${entity.text}"
            }
    }

    override fun toggleGhostMode() {
        _isGhostModeEnabled.update { !it }
        println("👻 Ghost Mode теперь: ${_isGhostModeEnabled.value}")
    }

    override fun markChatAsRead(chatId: Long, messageIds: List<Long>) {
        // ЕСЛИ РЕЖИМ ПРИЗРАКА ВКЛЮЧЕН — МЫ БЛОКИРУЕМ ПРОЧТЕНИЕ!
        if (_isGhostModeEnabled.value) {
            println("👻 Ghost Mode активен: прочтение заблокировано!")
            return
        }

        if (messageIds.isEmpty()) return

        // Если режим призрака выключен — шлем в TDLib команду отметить как прочитанное!
        val idsJson = messageIds.joinToString(",")
        tdlibClient.send(
            """
            {
                "@type": "viewMessages",
                "chat_id": $chatId,
                "message_ids": [$idsJson],
                "force_read": true
            }
        """.trimIndent()
        )
        println("👁 Ghost Mode выключен: отправлен статус прочтения для $chatId")
    }

    override suspend fun sendMessage(chatId: Long, text: String, useCrypto: Boolean, replyToMessageId: Long) {
        val finalText = if (useCrypto) {
            // ПРОВЕРЯЕМ: если рукопожатие еще не завершено — шлем запрос ключей вместо мусора!
            if (!cryptoLayer.isChatSecure(chatId)) {
                println("⚠️ E2EE: Ключ для чата $chatId еще не готов! Сначала завершите рукопожатие.")
                text // Шлем как обычный текст, либо блокируем
            } else {
                cryptoLayer.encryptAndHide(chatId, text)
            }
        } else {
            text
        }


        // ДОБАВЛЯЕМ link_preview_options, чтобы убить карточку GitHub!
        /*val request = """
            {
                "@type": "sendMessage",
                "chat_id": $chatId,
                "input_message_content": {
                    "@type": "inputMessageText",
                    "text": {
                        "@type": "formattedText",
                        "text": "$finalText"
                    },
                    "link_preview_options": {
                        "@type": "linkPreviewOptions",
                        "is_disabled": true
                    }
                }
            }
        """.trimIndent()*/
        val request = buildJsonObject {
            put("@type", "sendMessage")
            put("chat_id", chatId)

            // ЕСЛИ ЭТО ОТВЕТ — ДОБАВЛЯЕМ БЛОК REPLY_TO
            if (replyToMessageId != 0L) {
                put("reply_to", buildJsonObject {
                    put("@type", "inputMessageReplyToMessage")
                    put("message_id", replyToMessageId)
                })
            }

            put("input_message_content", buildJsonObject {
                put("@type", "inputMessageText")
                put("text", buildJsonObject {
                    put("@type", "formattedText")
                    put("text", finalText)
                })
                put("link_preview_options", buildJsonObject {
                    put("@type", "linkPreviewOptions")
                    put("is_disabled", true)
                })
            })
        }
        tdlibClient.send(request.toString())
        //tdlibClient.send(request)
    }

    override suspend fun requestKeyExchange(chatId: Long) {
        // Достаем наш публичный ключ в виде строки
        val myPubKeyHex = cryptoLayer.myKeyPair.second.toHex()

        // Отправляем спец-сообщение (Префикс 👻🔑)
        val text = "👻🔑 $myPubKeyHex"

        val request = """
            {
                "@type": "sendMessage",
                "chat_id": $chatId,
                "input_message_content": {
                    "@type": "inputMessageText",
                    "text": { "@type": "formattedText", "text": "$text" },
                    "link_preview_options": { "@type": "linkPreviewOptions", "is_disabled": true }
                }
            }
        """.trimIndent()
        tdlibClient.send(request)
    }

    override fun observeMyProfile(): Flow<MyProfile> {
        // Запрашиваем профиль КАЖДЫЙ РАЗ, когда UI на него подписывается!
        // Теперь никаких 404, потому что в настройки мы заходим уже залогиненными.
        tdlibClient.send("""{"@type": "getMe", "@extra": "get_me_avatar"}""")
        return _myProfile.asStateFlow()
    }

    override fun observeSearchResults(): Flow<List<PublicChat>> = _searchResults.asStateFlow()

    override fun searchPublicChats(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        println("📡 [3. Repo -> TDLib] Шлем команду searchPublicChats в C++: '$query'")

        // Отправляем запрос с фиксированной меткой!
        val request = """
            {
                "@type": "searchPublicChats",
                "query": "$query",
                "@extra": "search_public_$query" 
            }
        """.trimIndent()
        tdlibClient.send(request)
    }

    override fun observeMessageSearchResults(): Flow<List<Chat>> = _messageSearchResults.asStateFlow()

    override fun searchMessages(query: String) {
        if (query.isBlank()) {
            _messageSearchResults.value = emptyList()
            return
        }

        println("🔍 [ПОИСК СООБЩЕНИЙ] Отправляем запрос в TDLib: '$query'")
        val request = """
            {
                "@type": "searchMessages",
                "query": "$query",
                "offset_date": 0,
                "offset_chat_id": 0,
                "offset_message_id": 0,
                "limit": 20,
                "@extra": "search_msg_$query"
            }
        """.trimIndent()

        tdlibClient.send(request)
    }
    override suspend fun sendMedia(chatId: Long, bytes: ByteArray, extension: String, caption: String, useCrypto: Boolean, asDocument: Boolean, replyToMessageId: Long) {
        if (bytes.isEmpty()) return

        val finalCaption = if (useCrypto) cryptoLayer.encryptAndHide(chatId, caption) else caption

        // 1. Пишем файл ПРЯМО В ПАПКУ TDLIB (У ядра туда 100% есть права доступа!)
        val fs = FileSystem.SYSTEM
        val tempDir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "ghostgram_temp"
        if (!fs.exists(tempDir)) fs.createDirectories(tempDir)

        val ext = extension.lowercase().ifBlank { if (asDocument) "png" else "jpg" }
        val tempFile = tempDir / "ghost_${System.now().toEpochMilliseconds()}.$ext"
        fs.write(tempFile) { write(bytes) }
        val absolutePath = tempFile.toString().replace("\\", "/")

        val isVideo = ext in listOf("mp4", "mov", "mkv", "avi")

        val requestJson = buildJsonObject {
            put("@type", "sendMessage")
            put("chat_id", chatId)
            if (replyToMessageId != 0L) {
                put("reply_to", buildJsonObject {
                    put("@type", "inputMessageReplyToMessage")
                    put("message_id", replyToMessageId)
                })
            }

            put("input_message_content", buildJsonObject {
                when {
                    asDocument -> {
                        put("@type", "inputMessageDocument")
                        put("document", buildJsonObject {
                            put("@type", "inputDocument")
                            put("document", buildJsonObject { put("@type", "inputFileLocal"); put("path", absolutePath) })
                        })
                    }
                    isVideo -> {
                        // НОВОЕ: ОТПРАВКА ВИДЕО
                        put("@type", "inputMessageVideo")
                        put("video", buildJsonObject {
                            put("@type", "inputVideo") // ВОТ ЭТА ОБЕРТКА БЫЛА ПРОПУЩЕНА!
                            put("video", buildJsonObject {
                                put("@type", "inputFileLocal")
                                put("path", absolutePath)
                            })
                        })
                    }
                    else -> {
                        put("@type", "inputMessagePhoto")
                        put("photo", buildJsonObject {
                            put("@type", "inputPhoto")
                            put("photo", buildJsonObject { put("@type", "inputFileLocal"); put("path", absolutePath) })
                        })
                    }
                }

                if (finalCaption.isNotBlank()) {
                    put("caption", buildJsonObject {
                        put("@type", "formattedText")
                        put("text", finalCaption.trim())
                    })
                }
            })
        }


        println("📸 [ОТПРАВКА] Шлем: $requestJson")
        tdlibClient.send(requestJson.toString())
    }

    override suspend fun sendMediaAlbum(
        chatId: Long,
        media: List<Pair<ByteArray, String>>,
        caption: String,
        useCrypto: Boolean,
        replyToMessageId: Long
    ) {
        if (media.isEmpty()) return

        val finalCaption = if (useCrypto) cryptoLayer.encryptAndHide(chatId, caption) else caption

        val fs = FileSystem.SYSTEM
        val tempDir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "ghostgram_temp"
        if (!fs.exists(tempDir)) fs.createDirectories(tempDir)

        // Telegram разрешает максимум 10 файлов в одном альбоме (делим на чанки если больше)
        media.chunked(10).forEach { chunk ->

            // 1. Сохраняем файлы на диск и формируем элементы альбома
            val inputContents = chunk.mapIndexed { index, (bytes, extension) ->
                val ext = extension.lowercase().ifBlank { "jpg" }
                val tempFile = tempDir / "ghost_album_${System.now().toEpochMilliseconds()}_$index.$ext"
                fs.write(tempFile) { write(bytes) }
                val absolutePath = tempFile.toString().replace("\\", "/")
                val isVideo = ext in listOf("mp4", "mov", "mkv", "avi")

                buildJsonObject {
                    if (isVideo) {
                        put("@type", "inputMessageVideo")
                        put("video", buildJsonObject {
                            put("@type", "inputVideo")
                            put("video", buildJsonObject {
                                put("@type", "inputFileLocal")
                                put("path", absolutePath)
                            })
                        })
                    } else {
                        put("@type", "inputMessagePhoto")
                        put("photo", buildJsonObject {
                            put("@type", "inputPhoto")
                            put("photo", buildJsonObject {
                                put("@type", "inputFileLocal")
                                put("path", absolutePath)
                            })
                        })
                    }

                    // Подпись крепим ТОЛЬКО к первому элементу альбома (как в Telegram!)
                    if (index == 0 && finalCaption.isNotBlank()) {
                        put("caption", buildJsonObject {
                            put("@type", "formattedText")
                            put("text", finalCaption.trim())
                        })
                    }
                }
            }

            // 2. ОТПРАВЛЯЕМ КАК ЕДИНЫЙ АЛЬБОМ: sendMessageAlbum
            val requestJson = buildJsonObject {
                put("@type", "sendMessageAlbum")
                put("chat_id", chatId)

                if (replyToMessageId != 0L) {
                    put("reply_to", buildJsonObject {
                        put("@type", "inputMessageReplyToMessage")
                        put("message_id", replyToMessageId)
                    })
                }

                put("input_message_contents", buildJsonArray {
                    inputContents.forEach { add(it) }
                })
            }

            println("📸 [АЛЬБОМ] Шлем альбом из ${chunk.size} медиа: $requestJson")
            tdlibClient.send(requestJson.toString())
        }
    }

    override suspend fun deleteMessage(chatId: Long, messageId: Long, revoke: Boolean) {
        val request = """
            {
                "@type": "deleteMessages",
                "chat_id": $chatId,
                "message_ids": [$messageId],
                "revoke": $revoke
            }
        """.trimIndent()
        tdlibClient.send(request)
    }

    override suspend fun clearLocalCache(clearNormal: Boolean, clearAntiRevoke: Boolean) {
        if (clearNormal) messageDao.clearNormalMessages()
        if (clearAntiRevoke) messageDao.clearAntiRevokeMessages()
    }

    override suspend fun editMessageText(chatId: Long, messageId: Long, newText: String, useCrypto: Boolean) {
        // Если редактируем крипто-сообщение — шифруем новый текст!
        val finalText = if (useCrypto) {
            cryptoLayer.encryptAndHide(chatId, newText)
        } else {
            newText
        }

        val request = """
            {
                "@type": "editMessageText",
                "chat_id": $chatId,
                "message_id": $messageId,
                "input_message_content": {
                    "@type": "inputMessageText",
                    "text": {
                        "@type": "formattedText",
                        "text": "$finalText"
                    }
                }
            }
        """.trimIndent()

        tdlibClient.send(request)
    }

    override fun observeRecentStickers(): Flow<List<TelegramSticker>> = _recentStickers.asStateFlow()

    override fun loadRecentStickers() {
        // Просим у TDLib твои недавние стикеры!
        tdlibClient.send("""{"@type": "getRecentStickers", "is_attached": false}""")
    }

    override suspend fun sendSticker(chatId: Long, stickerFileId: Int, replyToMessageId: Long) {
        val requestJson = buildJsonObject {
            put("@type", "sendMessage")
            put("chat_id", chatId)

            if (replyToMessageId != 0L) {
                put("reply_to", buildJsonObject {
                    put("@type", "inputMessageReplyToMessage")
                    put("message_id", replyToMessageId)
                })
            }

            put("input_message_content", buildJsonObject {
                put("@type", "inputMessageSticker")

                // ТА САМАЯ МАТРЕШКА ДЛЯ СТИКЕРОВ!
                put("sticker", buildJsonObject {
                    put("@type", "inputSticker") // ОБЕРТКА!

                    put("sticker", buildJsonObject {
                        // Используем локальный ID, так как TDLib его уже знает!
                        put("@type", "inputFileId")
                        put("id", stickerFileId) // Передаем как Int
                    })
                })
            })
        }

        println("🎭 [СТИКЕР] Отправляем стикер в TDLib: $requestJson")
        tdlibClient.send(requestJson.toString())
    }

    override suspend fun sendVoiceNote(chatId: Long, filePath: String, replyToMessageId: Long) {
        val requestJson = buildJsonObject {
            put("@type", "sendMessage")
            put("chat_id", chatId)

            if (replyToMessageId != 0L) {
                put("reply_to", buildJsonObject {
                    put("@type", "inputMessageReplyToMessage")
                    put("message_id", replyToMessageId)
                })
            }

            put("input_message_content", buildJsonObject {
                put("@type", "inputMessageVoiceNote")
                put("voice_note", buildJsonObject {
                    put("@type", "inputVoiceNote") // НАША ЛЮБИМАЯ МАТРЕШКА
                    put("voice_note", buildJsonObject {
                        put("@type", "inputFileLocal")
                        put("path", filePath)
                    })
                })
            })
        }

        println("🎤 [ОТПРАВКА] Шлем голосовое: $requestJson")
        tdlibClient.send(requestJson.toString())
    }

    override suspend fun getChatFullProfile(chatId: Long): ChatFullProfile? {
        val chat = _chatsMap.value[chatId]

        // 💥 1. ЕСЛИ ЭТО ЛИЧНЫЙ ЧАТ 1-НА-1 (chatId > 0)
        if (chatId > 0) {
            val userObj = sendAndAwait("getUser", mapOf("user_id" to chatId))
            val fullInfoObj = sendAndAwait("getUserFullInfo", mapOf("user_id" to chatId))

            val bio = fullInfoObj?.get("bio")?.jsonObject?.get("text")?.jsonPrimitive?.content
                ?: fullInfoObj?.get("bio")?.jsonPrimitive?.content ?: ""
            val phone = userObj?.get("phone_number")?.jsonPrimitive?.content ?: ""
            val username = userObj?.get("usernames")?.jsonObject?.get("editable_username")?.jsonPrimitive?.content
                ?: userObj?.get("username")?.jsonPrimitive?.content ?: ""

            val firstName = userObj?.get("first_name")?.jsonPrimitive?.content ?: ""
            val lastName = userObj?.get("last_name")?.jsonPrimitive?.content ?: ""
            val title = "$firstName $lastName".trim().ifBlank { chat?.title ?: "Пользователь" }

            return ChatFullProfile(
                id = chatId,
                title = title,
                avatarPath = chat?.avatarPath,
                bio = bio,
                username = username,
                phoneNumber = phone,
                isGroup = false,
                isChannel = false,
                memberCount = 0
            )
        }
        // 💥 2. ЕСЛИ ЭТО ГРУППА ИЛИ СУПЕРГРУППА (chatId < 0)
        else {
            val chatObj = sendAndAwait("getChat", mapOf("chat_id" to chatId))
            val title = chatObj?.get("title")?.jsonPrimitive?.content ?: chat?.title ?: "Группа"

            // Пробуем достать супергруппу
            val typeObj = chatObj?.get("type")?.jsonObject
            val supergroupId = typeObj?.get("supergroup_id")?.jsonPrimitive?.longOrNull

            var memberCount = 0
            var description = "Групповой чат"

            if (supergroupId != null) {
                val supergroupFull = sendAndAwait("getSupergroupFullInfo", mapOf("supergroup_id" to supergroupId))
                memberCount = supergroupFull?.get("member_count")?.jsonPrimitive?.intOrNull ?: 0
                description = supergroupFull?.get("description")?.jsonPrimitive?.content ?: "Описание отсутствует"
            }

            return ChatFullProfile(
                id = chatId,
                title = title,
                avatarPath = chat?.avatarPath,
                bio = description,
                username = "",
                phoneNumber = "",
                isGroup = true,
                isChannel = false,
                memberCount = memberCount
            )
        }
    }

    override suspend fun getSharedMedia(chatId: Long, filterType: String, fromMessageId: Long): List<Message> {
        val filterObj = buildJsonObject { put("@type", filterType) }

        // 💥 Отправляем запрос в поисковый движок истории Telegram
        val response = sendAndAwait(
            requestType = "searchChatMessages",
            parameters = mapOf(
                "chat_id" to chatId,
                "query" to "",
                "filter" to filterObj,
                "from_message_id" to fromMessageId,
                "offset" to 0,
                "limit" to 50
            )
        ) ?: return emptyList()

        val messagesArray = response["messages"]?.jsonArray ?: return emptyList()

        return messagesArray.mapNotNull { msgElem ->
            val msgObj = msgElem.jsonObject
            val msgId = msgObj["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
            val contentObj = msgObj["content"]?.jsonObject
            val contentType = contentObj?.get("@type")?.jsonPrimitive?.content
            val date = msgObj["date"]?.jsonPrimitive?.intOrNull ?: 0

            when (contentType) {
                "messagePhoto" -> {
                    val sizes = contentObj["photo"]?.jsonObject?.get("sizes")?.jsonArray
                    val bigPhoto = sizes?.lastOrNull()?.jsonObject?.get("photo")?.jsonObject
                    val path = bigPhoto?.get("local")?.jsonObject?.get("path")?.jsonPrimitive?.content
                    val fileId = bigPhoto?.get("id")?.jsonPrimitive?.intOrNull

                    // Если файл еще не скачан — ставим в очередь загрузки
                    if (path.isNullOrBlank() && fileId != null) {
                        tdlibClient.send("""{"@type": "downloadFile", "file_id": $fileId, "priority": 1}""")
                    }

                    Message(id = msgId, chatId = chatId, senderName = "", text = "", photoPath = path, mediaType = MessageMediaType.PHOTO, date = date)
                }
                "messageVideo" -> {
                    val videoObj = contentObj["video"]?.jsonObject
                    val thumbObj = videoObj?.get("thumbnail")?.jsonObject?.get("file")?.jsonObject
                    val path = thumbObj?.get("local")?.jsonObject?.get("path")?.jsonPrimitive?.content
                    val duration = videoObj?.get("duration")?.jsonPrimitive?.intOrNull ?: 0
                    val videoPath = videoObj?.get("video")?.jsonObject?.get("local")?.jsonObject?.get("path")?.jsonPrimitive?.content

                    Message(id = msgId, chatId = chatId, senderName = "", text = "", photoPath = path, fileName = videoPath, fileExtraInfo = "$duration сек", mediaType = MessageMediaType.VIDEO, date = date)
                }
                "messageDocument" -> {
                    val docObj = contentObj["document"]?.jsonObject
                    val fileName = docObj?.get("file_name")?.jsonPrimitive?.content ?: "Документ"
                    val filePath = docObj?.get("document")?.jsonObject?.get("local")?.jsonObject?.get("path")?.jsonPrimitive?.content
                    Message(id = msgId, chatId = chatId, senderName = "", text = "", fileName = fileName, photoPath = filePath, mediaType = MessageMediaType.DOCUMENT, date = date)
                }
                "messageVoiceNote" -> {
                    val voiceObj = contentObj["voice_note"]?.jsonObject
                    val duration = voiceObj?.get("duration")?.jsonPrimitive?.intOrNull ?: 0
                    val filePath = voiceObj?.get("voice")?.jsonObject?.get("local")?.jsonObject?.get("path")?.jsonPrimitive?.content
                    Message(id = msgId, chatId = chatId, senderName = "", text = "", fileName = filePath, fileExtraInfo = "$duration сек", mediaType = MessageMediaType.VOICE, date = date)
                }
                else -> null
            }
        }
    }

    override fun openChat(chatId: Long) {
        // Говорим TDLib: "Юзер смотрит на этот чат! Дай инфу и начни скачивать всё необходимое!"
        tdlibClient.send("""{"@type": "openChat", "chat_id": $chatId}""")
        tdlibClient.send("""{"@type": "getChat", "chat_id": $chatId}""")
    }

    override fun closeChat(chatId: Long) {
        tdlibClient.send("""{"@type": "closeChat", "chat_id": $chatId}""")
    }

    private suspend fun sendAndAwait(
        requestType: String,
        parameters: Map<String, Any> = emptyMap(),
        timeoutMs: Long = 4000
    ): JsonObject? {
        val extraId = "req_${Clock.System.now().toEpochMilliseconds()}_${Random.nextInt(1000, 9999)}"

        // Собираем JSON запроса с меткой @extra
        val requestJson = buildJsonObject {
            put("@type", requestType)
            put("@extra", extraId)
            parameters.forEach { (key, value) ->
                when (value) {
                    is String -> put(key, value)
                    is Number -> put(key, value)
                    is Boolean -> put(key, value)
                    is JsonElement -> put(key, value)
                }
            }
        }.toString()

        return withTimeoutOrNull(timeoutMs.milliseconds) {
            // Заранее подписываемся на ожидание ответа с этим @extra
            val deferred = async {
                tdlibClient.updates
                    .filter { it.contains(extraId) }
                    .mapNotNull { raw ->
                        runCatching { jsonParser.parseToJsonElement(raw).jsonObject }.getOrNull()
                    }
                    .first { it["@extra"]?.jsonPrimitive?.content == extraId }
            }

            // Шлем команду в ядро
            tdlibClient.send(requestJson)

            // Ждем и возвращаем результат
            deferred.await()
        }
    }
}
