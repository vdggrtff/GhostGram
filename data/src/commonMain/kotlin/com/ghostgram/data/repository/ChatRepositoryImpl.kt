package com.ghostgram.data.repository

import com.ghostgram.core.crypto.CryptoLayer
import com.ghostgram.core.crypto.toHex
import com.ghostgram.core.database.dao.MessageDao
import com.ghostgram.core.tdlib.TelegramFlowClient
import com.ghostgram.core.tdlib.sendAndAwait
import com.ghostgram.data.mapper.toDomain
import com.ghostgram.data.repository.handlers.ChatUpdateHandler
import com.ghostgram.data.repository.handlers.DownloadTracker
import com.ghostgram.data.repository.handlers.MessageUpdateHandler
import com.ghostgram.data.repository.handlers.ProfileAndFileHandler
import com.ghostgram.data.repository.handlers.SearchUpdateHandler
import com.ghostgram.data.repository.handlers.StickerUpdateHandler
import com.ghostgram.data.repository.handlers.TdlibMediaSender
import com.ghostgram.data.repository.handlers.TdlibMessageParser
import com.ghostgram.data.repository.handlers.TdlibUpdateHandler
import entity.Chat
import entity.ChatFullProfile
import entity.Message
import entity.MyProfile
import entity.PublicChat
import entity.TelegramSticker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
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
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import repository.ChatRepository

class ChatRepositoryImpl(
    private val tdlibClient: TelegramFlowClient,
    private val messageDao: MessageDao,
    private val cryptoLayer: CryptoLayer,
) : ChatRepository {
    private val jsonParser = Json { ignoreUnknownKeys = true }
    private val repoScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Кэш чатов в памяти
    private val _chatsMap = MutableStateFlow<Map<Long, Chat>>(emptyMap())
    private val lastReadOutboxMap = mutableMapOf<Long, Long>()
    private val _isGhostModeEnabled = MutableStateFlow(true)

    // Вынесенные сервисы
    private val downloadTracker = DownloadTracker()
    private val messageParser = TdlibMessageParser(tdlibClient, downloadTracker)
    private val mediaSender = TdlibMediaSender(tdlibClient, cryptoLayer)

    // Потоки состояния
    private val _myProfile = MutableStateFlow(MyProfile())
    private val _searchResults = MutableStateFlow<List<PublicChat>>(emptyList())
    private val _messageSearchResults = MutableStateFlow<List<Chat>>(emptyList())
    private val _recentStickers = MutableStateFlow<List<TelegramSticker>>(emptyList())

    private val handlers: List<TdlibUpdateHandler> = listOf(
        SearchUpdateHandler(_searchResults, _messageSearchResults, _chatsMap),
        MessageUpdateHandler(
            messageDao,
            repoScope,
            tdlibClient,
            lastReadOutboxMap,
            downloadTracker,
            cryptoLayer
        ),
        ChatUpdateHandler(
            _chatsMap,
            lastReadOutboxMap,
            tdlibClient,
            messageDao,
            repoScope,
            downloadTracker
        ),
        ProfileAndFileHandler(
            _myProfile,
            _chatsMap,
            tdlibClient,
            messageDao,
            repoScope,
            downloadTracker
        ),
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
                } catch (e: Exception) {
                }
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

    override fun observeChat(chatId: Long): Flow<Chat?> = _chatsMap.map { it[chatId] }

    override suspend fun loadMoreMessages(chatId: Long, fromMessageId: Long) {
        // Просим еще 50 старых сообщений, начиная от fromMessageId
        tdlibClient.send(
            """
            {
                "@type": "getChatHistory",
                "chat_id": $chatId,
                "from_message_id": $fromMessageId,
                "offset": 0,
                "limit": 50,
                "only_local": false
            }
        """.trimIndent()
        )
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
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun sendMessage(chatId: Long, text: String) =
        mediaSender.sendText(chatId, text, false, 0L)

    override suspend fun sendMessage(
        chatId: Long,
        text: String,
        useCrypto: Boolean,
        replyToMessageId: Long,
    ) = mediaSender.sendText(chatId, text, useCrypto, replyToMessageId)

    override suspend fun sendMedia(
        chatId: Long,
        bytes: ByteArray,
        extension: String,
        caption: String,
        useCrypto: Boolean,
        asDocument: Boolean,
        replyToMessageId: Long,
    ) = mediaSender.sendMedia(
        chatId,
        bytes,
        extension,
        caption,
        useCrypto,
        asDocument,
        replyToMessageId
    )

    override suspend fun sendMediaAlbum(
        chatId: Long,
        media: List<Pair<ByteArray, String>>,
        caption: String,
        useCrypto: Boolean,
        replyToMessageId: Long,
    ) = mediaSender.sendMediaAlbum(chatId, media, caption, useCrypto, replyToMessageId)

    override suspend fun sendSticker(chatId: Long, stickerFileId: Int, replyToMessageId: Long) =
        mediaSender.sendSticker(chatId, stickerFileId, replyToMessageId)

    override suspend fun sendVoiceNote(chatId: Long, filePath: String, replyToMessageId: Long) =
        mediaSender.sendVoiceNote(chatId, filePath, replyToMessageId)

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

    override fun observeGhostMode(): Flow<Boolean> = _isGhostModeEnabled.asStateFlow()

    override fun markChatAsRead(chatId: Long, messageIds: List<Long>) {
        if (_isGhostModeEnabled.value || messageIds.isEmpty()) return
        val idsJson = messageIds.joinToString(",")
        tdlibClient.send("""{"@type": "viewMessages", "chat_id": $chatId, "message_ids": [$idsJson], "force_read": true}""")
    }

    override suspend fun requestKeyExchange(chatId: Long) {
        val myPubKeyHex = cryptoLayer.myKeyPair.second.toHex()
        tdlibClient.send(
            """{"@type": "sendMessage", "chat_id": $chatId, "input_message_content": {"@type": "inputMessageText", "text": {"@type": "formattedText", "text": "👻🔑 $myPubKeyHex"}, "link_preview_options": {"@type": "linkPreviewOptions", "is_disabled": true}}}"""
        )
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
        val request = """
            {
                "@type": "searchPublicChats",
                "query": "$query",
                "@extra": "search_public_$query" 
            }
        """.trimIndent()
        tdlibClient.send(request)
    }

    override fun observeMessageSearchResults(): Flow<List<Chat>> =
        _messageSearchResults.asStateFlow()

    override fun searchMessages(query: String) {
        if (query.isBlank()) {
            _messageSearchResults.value = emptyList()
            return
        }
        tdlibClient.send("""{"@type": "searchMessages", "query": "$query", "offset_date": 0, "offset_chat_id": 0, "offset_message_id": 0, "limit": 20, "@extra": "search_msg_$query"}""")
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

    override suspend fun editMessageText(
        chatId: Long,
        messageId: Long,
        newText: String,
        useCrypto: Boolean,
    ) {
        val finalText = if (useCrypto) cryptoLayer.encryptAndHide(chatId, newText) else newText
        tdlibClient.send(
            """{"@type": "editMessageText", "chat_id": $chatId, "message_id": $messageId, "input_message_content": {"@type": "inputMessageText", "text": {"@type": "formattedText", "text": "$finalText"}}}"""
        )
    }

    override fun observeRecentStickers(): Flow<List<TelegramSticker>> =
        _recentStickers.asStateFlow()

    override fun loadRecentStickers() {
        tdlibClient.send("""{"@type": "getRecentStickers", "is_attached": false}""")
    }

    override suspend fun getChatFullProfile(chatId: Long): ChatFullProfile? {
        val chat = _chatsMap.value[chatId]

        if (chatId > 0) {
            val userObj = tdlibClient.sendAndAwait("getUser", mapOf("user_id" to chatId))
            val fullInfoObj =
                tdlibClient.sendAndAwait("getUserFullInfo", mapOf("user_id" to chatId))

            val bio = fullInfoObj?.get("bio")?.jsonObject?.get("text")?.jsonPrimitive?.content
                ?: fullInfoObj?.get("bio")?.jsonPrimitive?.content ?: ""
            val phone = userObj?.get("phone_number")?.jsonPrimitive?.content ?: ""
            val username =
                userObj?.get("usernames")?.jsonObject?.get("editable_username")?.jsonPrimitive?.content
                    ?: userObj?.get("username")?.jsonPrimitive?.content ?: ""

            val firstName = userObj?.get("first_name")?.jsonPrimitive?.content ?: ""
            val lastName = userObj?.get("last_name")?.jsonPrimitive?.content ?: ""
            val title = "$firstName $lastName".trim().ifBlank { chat?.title ?: "Пользователь" }

            return ChatFullProfile(
                id = chatId, title = title, avatarPath = chat?.avatarPath,
                bio = bio, username = username, phoneNumber = phone
            )
        } else {
            val chatObj = tdlibClient.sendAndAwait("getChat", mapOf("chat_id" to chatId))
            val title = chatObj?.get("title")?.jsonPrimitive?.content ?: chat?.title ?: "Группа"
            val typeObj = chatObj?.get("type")?.jsonObject
            val supergroupId = typeObj?.get("supergroup_id")?.jsonPrimitive?.longOrNull

            var memberCount = 0
            var description = "Групповой чат"

            if (supergroupId != null) {
                val supergroupFull = tdlibClient.sendAndAwait(
                    "getSupergroupFullInfo",
                    mapOf("supergroup_id" to supergroupId)
                )
                memberCount = supergroupFull?.get("member_count")?.jsonPrimitive?.intOrNull ?: 0
                description = supergroupFull?.get("description")?.jsonPrimitive?.content
                    ?: "Описание отсутствует"
            }

            return ChatFullProfile(
                id = chatId, title = title, avatarPath = chat?.avatarPath,
                bio = description, isGroup = true, memberCount = memberCount
            )
        }
    }


    override suspend fun getSharedMedia(
        chatId: Long,
        filterType: String,
        fromMessageId: Long,
    ): List<Message> {
        val filterObj = buildJsonObject { put("@type", filterType) }
        val response = tdlibClient.sendAndAwait(
            requestType = "searchChatMessages",
            parameters = mapOf(
                "chat_id" to chatId, "query" to "", "filter" to filterObj,
                "from_message_id" to fromMessageId, "offset" to 0, "limit" to 50
            )
        ) ?: return emptyList()

        val messagesArray = response["messages"]?.jsonArray ?: return emptyList()
        return messagesArray.mapNotNull { elem ->
            messageParser.parse(elem.jsonObject, fallbackChatId = chatId)
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
}
