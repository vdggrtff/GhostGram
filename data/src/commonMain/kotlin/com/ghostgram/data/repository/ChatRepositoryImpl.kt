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
import com.ghostgram.data.repository.handlers.TdlibUpdateHandler
import entity.Chat
import entity.Message
import entity.MessageMediaType
import entity.MyProfile
import entity.PublicChat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.serializer
import okio.FileSystem
import okio.Path
import okio.SYSTEM
import repository.ChatRepository
import kotlin.time.Clock
import kotlin.time.Clock.System

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

    private val handlers: List<TdlibUpdateHandler> = listOf(
        SearchUpdateHandler(_searchResults, _messageSearchResults, _chatsMap),
        MessageUpdateHandler(messageDao, repoScope, tdlibClient, lastReadOutboxMap, downloadTracker, cryptoLayer),
        ChatUpdateHandler(_chatsMap, lastReadOutboxMap, tdlibClient, messageDao, repoScope, downloadTracker),
        ProfileAndFileHandler(_myProfile, _chatsMap, tdlibClient, messageDao, repoScope, downloadTracker),
    )


    init {
        tdlibClient.updates
            .onEach { rawJson ->
                try {
                    val jsonObject = jsonParser.parseToJsonElement(rawJson).jsonObject
                    val type = jsonObject["@type"]?.jsonPrimitive?.content ?: return@onEach

                    // 💥 Просто перебираем хэндлеры
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
                .filter { it.order > 0L } // 💥 ВЫКИДЫВАЕМ ВЕСЬ МУСОР ИЗ КЭША!
                .sortedByDescending { it.order } // 💥 Сортируем (свежие чаты сверху!)
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

        // 💥 3. ЕДИНЫЙ ПОТОК ИЗ ROOM С АВТОМАТИЧЕСКОЙ ПРОВЕРКОЙ GHOST MODE
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
                        isSending = entity.isSending
                    )
                }
            }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun sendMessage(chatId: Long, text: String) {
        tdlibClient.send("""{"@type": "sendMessage", "chat_id": $chatId, "input_message_content": {"@type": "inputMessageText", "text": {"@type": "formattedText", "text": "$text"}}}""")
    }

    override suspend fun getChatHistory(chatId: Long, limit: Int): String {
        // 💥 ТЕПЕРЬ МЫ БЕРЕМ ИСТОРИЮ ПРЯМО ИЗ БАЗЫ ДАННЫХ ROOM!
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
        // 💥 ЕСЛИ РЕЖИМ ПРИЗРАКА ВКЛЮЧЕН — МЫ БЛОКИРУЕМ ПРОЧТЕНИЕ!
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

    override suspend fun sendMessage(chatId: Long, text: String, useCrypto: Boolean) {
        val finalText = if (useCrypto) {
            // 💥 ПРОВЕРЯЕМ: если рукопожатие еще не завершено — шлем запрос ключей вместо мусора!
            if (!cryptoLayer.isChatSecure(chatId)) {
                println("⚠️ E2EE: Ключ для чата $chatId еще не готов! Сначала завершите рукопожатие.")
                text // Шлем как обычный текст, либо блокируем
            } else {
                cryptoLayer.encryptAndHide(chatId, text)
            }
        } else {
            text
        }


        // 💥 ДОБАВЛЯЕМ link_preview_options, чтобы убить карточку GitHub!
        val request = """
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
        """.trimIndent()
        tdlibClient.send(request)
    }

    override suspend fun requestKeyExchange(chatId: Long) {
        // Достаем наш публичный ключ в виде строки
        val myPubKeyHex = cryptoLayer.myKeyPair.second.toHex()

        // 💥 Отправляем спец-сообщение (Префикс 👻🔑)
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
        // 💥 Запрашиваем профиль КАЖДЫЙ РАЗ, когда UI на него подписывается!
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

        // 💥 Отправляем запрос с фиксированной меткой!
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
    override suspend fun sendMedia(chatId: Long, bytes: ByteArray, extension: String, caption: String, useCrypto: Boolean, asDocument: Boolean) {
        if (bytes.isEmpty()) return

        val finalCaption = if (useCrypto) cryptoLayer.encryptAndHide(chatId, caption) else caption

        // 💥 1. Пишем файл ПРЯМО В ПАПКУ TDLIB (У ядра туда 100% есть права доступа!)
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
                        // 💥 НОВОЕ: ОТПРАВКА ВИДЕО
                        put("@type", "inputMessageVideo")
                        put("video", buildJsonObject {
                            put("@type", "inputVideo") // 💥 ВОТ ЭТА ОБЕРТКА БЫЛА ПРОПУЩЕНА!
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
}
