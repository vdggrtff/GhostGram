package com.ghostgram.data.repository.handlers

import com.ghostgram.core.tdlib.TelegramFlowClient
import com.ghostgram.core.tdlib.sendAndAwait
import usecase.AntiSpamFilter
import com.ghostgram.data.repository.utils.ChatMemoryCache
import entity.Chat
import entity.Message
import entity.PublicChat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

class SearchUpdateHandler(
    private val chatCache: ChatMemoryCache,
    private val tdlibClient: TelegramFlowClient,
    private val scope: CoroutineScope
) : TdlibUpdateHandler {

    private val _searchResults = MutableStateFlow<List<PublicChat>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _messageSearchResults = MutableStateFlow<List<Chat>>(emptyList())
    val messageSearchResults = _messageSearchResults.asStateFlow()
    override fun handle(type: String, jsonObject: JsonObject): Boolean {
        val extra = jsonObject["@extra"]?.jsonPrimitive?.content

        // 💥 1. ЛОВИМ ОТВЕТ ПО МЕТКЕ search_public:
        if (extra?.startsWith("search_public") == true && type == "chats") {
            val chatIds = jsonObject["chat_ids"]?.jsonArray ?: return true
            println("✅ [ПОИСК] TDLib вернул ${chatIds.size} публичных каналов!")

            if (chatIds.isEmpty()) {
                _searchResults.value = emptyList()
                return true
            }

            scope.launch {
                val chatsList = mutableListOf<PublicChat>()

                chatIds.forEach { elem ->
                    val id = elem.jsonPrimitive.longOrNull ?: return@forEach
                    val cachedChat = chatCache.getChat(id)

                    if (cachedChat != null) {
                        if (!AntiSpamFilter.isSpam(cachedChat.title, "public")) {
                            chatsList.add(
                                PublicChat(
                                    id = cachedChat.id,
                                    title = cachedChat.title,
                                    username = "Публичный канал",
                                    avatarPath = cachedChat.avatarPath
                                )
                            )
                        }
                    } else {
                        // 💥 ЗАПРАШИВАЕМ ИНФО О ЧАТЕ У TDLIB ВМЕСТО "Загрузка..."!
                        val chatObj = tdlibClient.sendAndAwait(
                            requestType = "getChat",
                            parameters = mapOf("chat_id" to id),
                            timeoutMs = 1500
                        )

                        val title = chatObj?.get("title")?.jsonPrimitive?.content ?: "Канал"
                        val smallPhoto = chatObj?.get("photo")?.jsonObject?.get("small")?.jsonObject
                        val avatarPath = smallPhoto?.get("local")?.jsonObject?.get("path")?.jsonPrimitive?.content
                        val fileId = smallPhoto?.get("id")?.jsonPrimitive?.intOrNull

                        if (avatarPath.isNullOrBlank() && fileId != null && fileId != 0) {
                            tdlibClient.send("""{"@type": "downloadFile", "file_id": $fileId, "priority": 16}""")
                        }

                        if (!AntiSpamFilter.isSpam(title, "public")) {
                            chatsList.add(
                                PublicChat(
                                    id = id,
                                    title = title,
                                    username = "Публичный канал",
                                    avatarPath = avatarPath
                                )
                            )
                        }
                    }
                }

                _searchResults.value = chatsList
                println("✅ [ПОИСК] Успешно загружено публичных чатов для UI: ${chatsList.size}")
            }
            return true
        }

        // 💥 2. ОШИБКА ПОИСКА СООБЩЕНИЙ:
        if (extra?.startsWith("search_msg_") == true && type == "error") {
            println("❌ [ПОИСК СООБЩЕНИЙ] ОШИБКА: $jsonObject")
            return true
        }

        // 💥 3. ОТВЕТ ПОИСКА СООБЩЕНИЙ:
        if (extra?.startsWith("search_msg_") == true && (type == "messages" || type == "foundMessages")) {
            val messagesArray = jsonObject["messages"]?.jsonArray ?: return true
            println("✅ [ПОИСК СООБЩЕНИЙ] Найдено: ${messagesArray.size} сообщений")

            val results = mutableListOf<Chat>()
            messagesArray.forEach { msgElement ->
                val msgObj = msgElement.jsonObject
                val chatId = msgObj["chat_id"]?.jsonPrimitive?.longOrNull ?: return@forEach
                val msgId = msgObj["id"]?.jsonPrimitive?.longOrNull ?: return@forEach
                val isOutgoing = msgObj["is_outgoing"]?.jsonPrimitive?.booleanOrNull ?: false
                val date = msgObj["date"]?.jsonPrimitive?.intOrNull ?: 0
                val contentObj = msgObj["content"]?.jsonObject

                val msgType = contentObj?.get("@type")?.jsonPrimitive?.content
                val text = when (msgType) {
                    "messageText" -> contentObj["text"]?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""
                    "messagePhoto" -> "📷 Фотография"
                    "messageVideo", "messageVideoNote" -> "🎥 Видео"
                    "messageDocument" -> "📄 Документ"
                    "messageVoiceNote" -> "🎤 Голосовое сообщение"
                    "messageSticker" -> "✨ Стикер"
                    else -> "[Медиа]"
                }

                val chat = chatCache.getChat(chatId)
                val chatTitle = chat?.title ?: "Найденный диалог"
                val chatAvatar = chat?.avatarPath

                val foundMsg = Message(id = msgId, chatId = chatId, senderName = if (isOutgoing) "Вы" else chatTitle, text = text, date = date)

                val resultChat = chat?.copy(lastMessage = foundMsg) ?: Chat(
                    id = chatId, title = chatTitle, unreadCount = 0, lastMessage = foundMsg, avatarPath = chatAvatar
                )
                results.add(resultChat)
            }

            _messageSearchResults.value = results
            return true
        }

        return false
    }
}
