package com.ghostgram.data.repository

import com.ghostgram.core.crypto.CryptoLayer
import com.ghostgram.core.crypto.toHex
import com.ghostgram.core.database.dao.MessageDao
import com.ghostgram.core.tdlib.TelegramFlowClient
import com.ghostgram.core.tdlib.sendAndAwait
import com.ghostgram.data.mapper.toDomain
import com.ghostgram.data.repository.handlers.ChatStatusAndActionHandler
import com.ghostgram.data.repository.handlers.ChatUpdateHandler
import com.ghostgram.data.repository.handlers.MessageUpdateHandler
import com.ghostgram.data.repository.handlers.ProfileAndFileHandler
import com.ghostgram.data.repository.handlers.SearchUpdateHandler
import com.ghostgram.data.repository.handlers.StickerUpdateHandler
import com.ghostgram.data.repository.handlers.TdlibUpdateHandler
import com.ghostgram.data.repository.utils.ChatMemoryCache
import com.ghostgram.data.repository.utils.ChatProfileDataLoader
import com.ghostgram.data.repository.utils.DownloadTracker
import com.ghostgram.data.repository.utils.SearchStemmer
import com.ghostgram.data.repository.utils.TdlibLinkResolver
import com.ghostgram.data.repository.utils.TdlibMediaSender
import com.ghostgram.data.repository.utils.TdlibMessageParser
import com.ghostgram.data.repository.utils.UserMemoryCache
import entity.Chat
import entity.Message
import entity.MyProfile
import entity.TelegramSticker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import repository.ChatRepository

class ChatRepositoryImpl(
    private val tdlibClient: TelegramFlowClient,
    private val messageDao: MessageDao,
    private val cryptoLayer: CryptoLayer,
) : ChatRepository {
    private val jsonParser = Json { ignoreUnknownKeys = true }
    private val repoScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // 💥 1. ВЫНЕСЕННЫЙ КЭШ И СЕРВИСЫ
    private val chatCache = ChatMemoryCache()
    private val downloadTracker = DownloadTracker()
    private val messageParser = TdlibMessageParser(tdlibClient, downloadTracker)
    private val mediaSender = TdlibMediaSender(tdlibClient, cryptoLayer)
    private val linkResolver = TdlibLinkResolver(tdlibClient)
    private val profileLoader = ChatProfileDataLoader(tdlibClient, messageParser, chatCache)

    // 💥 2. ХЭНДЛЕРЫ-ХОЗЯЕВА СВОИХ СОСТОЯНИЙ
    private val statusAndActionHandler = ChatStatusAndActionHandler(repoScope)
    private val searchHandler = SearchUpdateHandler(chatCache)
    private val profileHandler =
        ProfileAndFileHandler(chatCache, tdlibClient, messageDao, repoScope, downloadTracker)
    private val stickerHandler = StickerUpdateHandler(tdlibClient, downloadTracker)
    private val userCache = UserMemoryCache()

    private val _isGhostModeEnabled = MutableStateFlow(true)

    private val handlers: List<TdlibUpdateHandler> = listOf(
        statusAndActionHandler,
        searchHandler,
        MessageUpdateHandler(
            messageDao,
            repoScope,
            tdlibClient,
            chatCache,
            downloadTracker,
            cryptoLayer,
            userCache
        ),
        ChatUpdateHandler(chatCache, tdlibClient, messageDao, repoScope, downloadTracker),
        profileHandler,
        stickerHandler,
    )

    init {
        tdlibClient.updates
            .onEach { rawJson ->
                try {
                    val jsonObject = jsonParser.parseToJsonElement(rawJson).jsonObject
                    val type = jsonObject["@type"]?.jsonPrimitive?.content ?: return@onEach
                    for (handler in handlers) {
                        if (handler.handle(type, jsonObject)) break
                    }
                } catch (e: Exception) {
                    println("❌ [REPO CRASH] Ошибка обновления: ${e.message}")
                }
            }
            .launchIn(repoScope)
    }

    // -------------------------------------------------------------------------
    // ЧАТЫ И СООБЩЕНИЯ
    // -------------------------------------------------------------------------
    override fun observeChats(): Flow<List<Chat>> {
        tdlibClient.send("""{"@type": "getMe"}""")
        tdlibClient.send("""{"@type": "loadChats", "chat_list": {"@type": "chatListMain"}, "limit": 30}""")
        return chatCache.chatsFlow.map { map ->
            map.values.filter { it.order > 0L }.sortedByDescending { it.order }
        }
    }

    override fun observeChat(chatId: Long): Flow<Chat?> = chatCache.chatsFlow.map { it[chatId] }

    override suspend fun loadMoreMessages(chatId: Long, fromMessageId: Long) {
        val offset = if (fromMessageId != 0L) -20 else 0
        tdlibClient.send("""{"@type": "getChatHistory", "chat_id": $chatId, "from_message_id": $fromMessageId, "offset": $offset, "limit": 50, "only_local": false}""")
    }

    override fun observeMessages(chatId: Long): Flow<List<Message>> {
        tdlibClient.send("""{"@type": "openChat", "chat_id": $chatId}""")
        repoScope.launch {
            tdlibClient.send("""{"@type": "getChatHistory", "chat_id": $chatId, "from_message_id": 0, "offset": 0, "limit": 50, "only_local": false}""")
        }
        return messageDao.observeMessages(chatId)
            .onEach { if (it.isNotEmpty()) markChatAsRead(chatId, it.map { m -> m.id }) }
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)
    }

    // -------------------------------------------------------------------------
    // ДЕЛЕГИРОВАНИЕ В СЕРВИСЫ (В 1 СТРОЧКУ!)
    // -------------------------------------------------------------------------
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
        ext: String,
        cap: String,
        crypto: Boolean,
        doc: Boolean,
        replyId: Long,
    ) = mediaSender.sendMedia(chatId, bytes, ext, cap, crypto, doc, replyId)

    override suspend fun sendMediaAlbum(
        chatId: Long,
        media: List<Pair<ByteArray, String>>,
        cap: String,
        crypto: Boolean,
        replyId: Long,
    ) = mediaSender.sendMediaAlbum(chatId, media, cap, crypto, replyId)

    override suspend fun sendSticker(chatId: Long, stickerFileId: Int, replyToId: Long) =
        mediaSender.sendSticker(chatId, stickerFileId, replyToId)

    override suspend fun sendVoiceNote(chatId: Long, filePath: String, replyToId: Long) =
        mediaSender.sendVoiceNote(chatId, filePath, replyToId)

    override suspend fun getChatFullProfile(chatId: Long) = profileLoader.getFullProfile(chatId)
    override suspend fun getSharedMedia(chatId: Long, filterType: String, fromMessageId: Long) =
        profileLoader.getSharedMedia(chatId, filterType, fromMessageId)

    override suspend fun getPinnedMessage(chatId: Long) = profileLoader.getPinnedMessage(chatId)

    override fun observeUserStatus(userId: Long) = statusAndActionHandler.observeUserStatus(userId)
    override fun observeChatTyping(chatId: Long) = statusAndActionHandler.observeChatTyping(chatId)
    override fun sendChatAction(chatId: Long) =
        tdlibClient.send("""{"@type": "sendChatAction", "chat_id": $chatId, "action": {"@type": "chatActionTyping"}}""")

    override suspend fun resolveUsernameToChatId(username: String) =
        linkResolver.resolveUsername(username)

    override suspend fun checkInviteLink(inviteLink: String) = linkResolver.checkInvite(inviteLink)
    override suspend fun joinChatByInvite(inviteLink: String) = linkResolver.joinChat(inviteLink)

    override fun observeMyProfile(): Flow<MyProfile> {
        tdlibClient.send("""{"@type": "getMe", "@extra": "get_me_avatar"}""")
        return profileHandler.myProfile
    }

    override fun observeRecentStickers(): Flow<List<TelegramSticker>> =
        stickerHandler.recentStickers

    override suspend fun getReactionEffectFilePath(chatId: Long, messageId: Long, emoji: String): String? {
        println("🎬 [REPO 1] Запрашиваем анимацию getEmojiReaction для '$emoji'...")

        // 💥 ВОТ ОН, НАСТОЯЩИЙ МЕТОД TDLIB ДЛЯ САЛЮТОВ РЕАКЦИЙ:
        val response = tdlibClient.sendAndAwait(
            requestType = "getEmojiReaction",
            parameters = mapOf("emoji" to emoji),
            timeoutMs = 4000
        )

        if (response == null || response["@type"]?.jsonPrimitive?.content == "error") {
            println("❌ [REPO ERROR] TDLib ошибка getEmojiReaction: $response")
            return null
        }

        println("🎬 [REPO 2] emojiReaction успешно получен от Telegram!")

        // 💥 Достаем effect_animation (или around_animation)
        val effectAnimation = response["effect_animation"]?.jsonObject
            ?: response["around_animation"]?.jsonObject
            ?: response["activate_animation"]?.jsonObject

        val stickerFileObj = effectAnimation?.get("sticker")?.jsonObject
        val fileId = stickerFileObj?.get("id")?.jsonPrimitive?.intOrNull

        if (fileId == null) {
            println("❌ [REPO ERROR] Не найден file_id в effect_animation: $effectAnimation")
            return null
        }

        var localPath = stickerFileObj["local"]?.jsonObject?.get("path")?.jsonPrimitive?.content
        val isCompleted = stickerFileObj["local"]?.jsonObject?.get("is_downloading_completed")?.jsonPrimitive?.booleanOrNull ?: false

        // Если .tgs файл еще не скачан — качаем его
        if (!isCompleted || localPath.isNullOrBlank()) {
            println("📥 [REPO 3] Скачиваем фирменный .tgs файл эффекта fileId=$fileId...")
            val downloadRes = tdlibClient.sendAndAwait(
                requestType = "downloadFile",
                parameters = mapOf(
                    "file_id" to fileId,
                    "priority" to 32,
                    "offset" to 0,
                    "limit" to 0,
                    "synchronous" to true
                ),
                timeoutMs = 8000
            )
            localPath = downloadRes?.get("local")?.jsonObject?.get("path")?.jsonPrimitive?.content
        }

        println("✅ [REPO 4] Путь к .tgs эффекту Telegram: '$localPath'")
        return localPath
    }

    override fun loadRecentStickers() =
        tdlibClient.send("""{"@type": "getRecentStickers", "is_attached": false}""")

    override fun observeSearchResults() = searchHandler.searchResults
    override fun observeMessageSearchResults() = searchHandler.messageSearchResults
    override fun searchPublicChats(query: String) =
        tdlibClient.send("""{"@type": "searchPublicChats", "query": "$query"}""")

    override fun searchMessages(query: String) =
        tdlibClient.send("""{"@type": "searchMessages", "query": "$query", "offset_date": 0, "offset_chat_id": 0, "offset_message_id": 0, "limit": 20}""")

    override suspend fun toggleChatMute(chatId: Long, isMuted: Boolean) {
        val muteFor = if (isMuted) 0 else 2147483647
        tdlibClient.send("""{"@type": "setChatNotificationSettings", "chat_id": $chatId, "notification_settings": {"@type": "chatNotificationSettings", "use_default_mute_for": false, "mute_for": $muteFor}}""")
    }

    override suspend fun toggleReaction(
        chatId: Long,
        messageId: Long,
        emoji: String,
        isChosen: Boolean,
    ) {
        if (isChosen) {
            // 💥 1. СНИМАЕМ РЕАКЦИЮ (БЕЗ is_big, чтобы TDLib не падал с 400 ошибкой!):
            tdlibClient.send("""
                {
                    "@type": "removeMessageReaction",
                    "chat_id": $chatId,
                    "message_id": $messageId,
                    "reaction_type": {"@type": "reactionTypeEmoji", "emoji": "$emoji"}
                }
            """.trimIndent())
        } else {
            // 💥 2. ЕСЛИ МЕНЯЕМ РЕАКЦИЮ: проверяем, была ли уже выбрана другая реакция
            val currentMsg = messageDao.observeMessages(chatId).firstOrNull()?.find { it.id == messageId }
            val oldReactions = currentMsg?.reactionsJson?.let {
                runCatching { jsonParser.decodeFromString<List<entity.MessageReactionItem>>(it) }.getOrNull()
            }
            val previousChosenEmoji = oldReactions?.firstOrNull { it.isChosen }?.emoji

            // Если уже стоял другой смайлик — сначала снимаем его!
            if (!previousChosenEmoji.isNullOrBlank() && previousChosenEmoji != emoji) {
                tdlibClient.send("""
                    {
                        "@type": "removeMessageReaction",
                        "chat_id": $chatId,
                        "message_id": $messageId,
                        "reaction_type": {"@type": "reactionTypeEmoji", "emoji": "$previousChosenEmoji"}
                    }
                """.trimIndent())
            }

            // Ставим новую реакцию
            tdlibClient.send("""
                {
                    "@type": "addMessageReaction",
                    "chat_id": $chatId,
                    "message_id": $messageId,
                    "reaction_type": {"@type": "reactionTypeEmoji", "emoji": "$emoji"},
                    "is_big": true,
                    "update_recent_reactions": true
                }
            """.trimIndent())
        }
    }

    override suspend fun deleteMessage(chatId: Long, messageId: Long, revoke: Boolean) {
        tdlibClient.send("""{"@type": "deleteMessages", "chat_id": $chatId, "message_ids": [$messageId], "revoke": $revoke}""")
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
        val text = if (useCrypto) cryptoLayer.encryptAndHide(chatId, newText) else newText
        tdlibClient.send("""{"@type": "editMessageText", "chat_id": $chatId, "message_id": $messageId, "input_message_content": {"@type": "inputMessageText", "text": {"@type": "formattedText", "text": "$text"}}}""")
    }

    override suspend fun requestKeyExchange(chatId: Long) {
        val myPubKeyHex = cryptoLayer.myKeyPair.second.toHex()
        tdlibClient.send("""{"@type": "sendMessage", "chat_id": $chatId, "input_message_content": {"@type": "inputMessageText", "text": {"@type": "formattedText", "text": "👻🔑 $myPubKeyHex"}, "link_preview_options": {"@type": "linkPreviewOptions", "is_disabled": true}}}""")
    }

    override suspend fun getChatHistory(chatId: Long, limit: Int): String {
        return messageDao.observeMessages(chatId).firstOrNull()?.takeLast(limit)
            ?.filter { it.fileExtraInfo != "ENCRYPTED" }
            ?.joinToString("\n") { "${it.senderName}: ${it.text}" } ?: ""
    }

    override fun toggleGhostMode() {
        _isGhostModeEnabled.update { !it }
    }

    override fun observeGhostMode(): Flow<Boolean> = _isGhostModeEnabled.asStateFlow()

    override fun markChatAsRead(chatId: Long, messageIds: List<Long>) {
        if (_isGhostModeEnabled.value || messageIds.isEmpty()) return
        tdlibClient.send(
            """{"@type": "viewMessages", "chat_id": $chatId, "message_ids": [${
                messageIds.joinToString(
                    ","
                )
            }], "force_read": true}"""
        )
    }

    override fun openChat(chatId: Long) {
        tdlibClient.send("""{"@type": "openChat", "chat_id": $chatId}""")
        tdlibClient.send("""{"@type": "getChat", "chat_id": $chatId}""")
        if (chatId > 0) tdlibClient.send("""{"@type": "getUser", "user_id": $chatId}""")
    }

    override fun closeChat(chatId: Long) =
        tdlibClient.send("""{"@type": "closeChat", "chat_id": $chatId}""")

    override suspend fun getAvailableReactions(chatId: Long, messageId: Long): List<String> {
        val response = tdlibClient.sendAndAwait(
            requestType = "getMessageAvailableReactions",
            parameters = mapOf(
                "chat_id" to chatId,
                "message_id" to messageId,
                "row_size" to 8
            ),
            timeoutMs = 3000
        ) ?: return listOf("👍", "❤️", "🔥", "😂", "👏") // Дефолт при оффлайне

        val topReactions = response["top_reactions"]?.jsonArray ?: return emptyList()

        return topReactions.mapNotNull { elem ->
            val typeObj = elem.jsonObject["type"]?.jsonObject
            typeObj?.get("emoji")?.jsonPrimitive?.content
        }
    }

    // 💥 2. НАШ ГИБРИДНЫЙ ПОИСК (SQLITE LIKE + TDLIB)
    override suspend fun searchMessagesInChat(
        chatId: Long,
        query: String,
        fromMessageId: Long,
    ): List<Message> {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return emptyList()

        val stem = SearchStemmer.trimEnding(cleanQuery)

        return coroutineScope {
            // 🌐 Облако TDLib:
            val cloudJob = async {
                val filterObj = buildJsonObject { put("@type", "searchMessagesFilterEmpty") }
                val response = tdlibClient.sendAndAwait(
                    requestType = "searchChatMessages",
                    parameters = mapOf(
                        "chat_id" to chatId,
                        "query" to cleanQuery,
                        "filter" to filterObj,
                        "from_message_id" to fromMessageId,
                        "offset" to 0,
                        "limit" to 50
                    ),
                    timeoutMs = 4000
                ) ?: return@async emptyList()

                val messagesArray = response["messages"]?.jsonArray ?: return@async emptyList()
                messagesArray.mapNotNull { elem ->
                    messageParser.parse(elem.jsonObject, fallbackChatId = chatId)
                }
            }

            // 💾 Локальная база SQLite:
            val localJob = async {
                val localEntities = messageDao.searchLocalMessages(chatId, cleanQuery, stem)
                localEntities.map { it.toDomain() }
            }

            val cloudResults = runCatching { cloudJob.await() }.getOrDefault(emptyList())
            val localResults = runCatching { localJob.await() }.getOrDefault(emptyList())

            (localResults + cloudResults)
                .distinctBy { it.id }
                .sortedWith(compareByDescending<Message> { it.date }.thenByDescending { it.id })
        }
    }
}