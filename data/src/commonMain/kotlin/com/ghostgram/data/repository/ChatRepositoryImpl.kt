package com.ghostgram.data.repository

import com.ghostgram.core.crypto.CryptoLayer
import com.ghostgram.core.database.dao.MessageDao
import com.ghostgram.core.tdlib.TelegramFlowClient
import com.ghostgram.data.repository.handlers.ChatUpdateHandler
import com.ghostgram.data.repository.handlers.DownloadTracker
import com.ghostgram.data.repository.handlers.MessageUpdateHandler
import com.ghostgram.data.repository.handlers.ProfileAndFileHandler
import com.ghostgram.data.repository.handlers.TdlibUpdateHandler
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
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import repository.ChatRepository

class ChatRepositoryImpl(
    private val tdlibClient: TelegramFlowClient,
    private val messageDao: MessageDao,
    private val cryptoLayer: CryptoLayer,
) : ChatRepository {

    private val jsonParser = Json { ignoreUnknownKeys = true }

    // Потокобезопасный кэш чатов в памяти: Map<ChatId, Chat>
    private val _chatsMap = MutableStateFlow<Map<Long, Chat>>(emptyMap())

    private val _messagesState = MutableStateFlow<Map<Long, List<Message>>>(emptyMap())
    private val repoScope = CoroutineScope(Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    private val _myAvatarPath = MutableStateFlow<String?>(null)

    private val _isGhostModeEnabled = MutableStateFlow(true)

    private val lastReadOutboxMap = mutableMapOf<Long, Long>()

    override fun observeGhostMode(): Flow<Boolean> = _isGhostModeEnabled.asStateFlow()
    private val downloadTracker = DownloadTracker()

    private val handlers: List<TdlibUpdateHandler> = listOf(
        MessageUpdateHandler(messageDao, repoScope, tdlibClient, lastReadOutboxMap, downloadTracker, cryptoLayer),
        ChatUpdateHandler(_chatsMap, lastReadOutboxMap, tdlibClient, messageDao, repoScope, downloadTracker),
        ProfileAndFileHandler(_myAvatarPath, _chatsMap, tdlibClient, messageDao, repoScope, downloadTracker)
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
                        isRead = entity.isRead
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
        // 💥 ЕСЛИ ВКЛЮЧЕН КРИПТО-РЕЖИМ — ШИФРУЕМ!
        val finalText = if (useCrypto) {
            cryptoLayer.encryptAndHide(text, cryptoLayer.TEST_SHARED_KEY)
        } else {
            text
        }

        val request = """
            {
                "@type": "sendMessage",
                "chat_id": $chatId,
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
}
