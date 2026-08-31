package com.ghostgram.data.repository

import com.ghostgram.core.tdlib.TelegramFlowClient
import entity.Chat
import entity.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
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
) : ChatRepository {

    private val jsonParser = Json { ignoreUnknownKeys = true }

    // Потокобезопасный кэш чатов в памяти: Map<ChatId, Chat>
    private val _chatsMap = MutableStateFlow<Map<Long, Chat>>(emptyMap())

    // Кэш сообщений: Map<ChatId, List<Message>>
    private val _messagesMap = MutableStateFlow<Map<Long, List<Message>>>(emptyMap())

    init {
        // Запускаем фонового слушателя апдейтов от TDLib
        tdlibClient.updates
            .onEach { rawJson -> parseTdlibUpdate(rawJson) }
            .launchIn(CoroutineScope(Dispatchers.Default))
    }

    private fun parseTdlibUpdate(rawJson: String) {
        try {
            val jsonObject = jsonParser.parseToJsonElement(rawJson).jsonObject
            val type = jsonObject["@type"]?.jsonPrimitive?.content ?: return

            when (type) {
                // Загрузка чатов
                "updateNewChat" -> {
                    val chatObj = jsonObject["chat"]?.jsonObject ?: return
                    val id = chatObj["id"]?.jsonPrimitive?.longOrNull ?: return
                    val title = chatObj["title"]?.jsonPrimitive?.content ?: "Без названия"
                    val unreadCount = chatObj["unread_count"]?.jsonPrimitive?.intOrNull ?: 0

                    val chat =
                        Chat(id = id, title = title, unreadCount = unreadCount, lastMessage = null)
                    _chatsMap.update { it + (id to chat) }
                }

                // 💥 Пришло новое сообщение (входящее или исходящее!)
                "updateNewMessage" -> {
                    val messageObj = jsonObject["message"]?.jsonObject ?: return
                    val chatId = messageObj["chat_id"]?.jsonPrimitive?.longOrNull ?: return
                    val messageId = messageObj["id"]?.jsonPrimitive?.longOrNull ?: return
                    val isOutgoing =
                        messageObj["is_outgoing"]?.jsonPrimitive?.booleanOrNull ?: false

                    val contentObj = messageObj["content"]?.jsonObject
                    val text =
                        contentObj?.get("text")?.jsonObject?.get("text")?.jsonPrimitive?.content
                            ?: ""

                    if (text.isNotBlank()) {
                        val newMessage = Message(
                            id = messageId,
                            chatId = chatId,
                            senderName = if (isOutgoing) "Вы" else "Собеседник",
                            text = text,
                            isOutgoing = isOutgoing
                        )

                        _messagesMap.update { current ->
                            val currentList = current[chatId] ?: emptyList()
                            current + (chatId to (currentList + newMessage))
                        }
                    }
                }

                "messages" -> {
                    val messagesArray = jsonObject["messages"]?.jsonArray ?: return
                    if (messagesArray.isEmpty()) return

                    // Достаем ID чата из первого сообщения
                    val firstMsg = messagesArray[0].jsonObject
                    val chatId = firstMsg["chat_id"]?.jsonPrimitive?.longOrNull ?: return

                    val parsedMessages = messagesArray.mapNotNull { msgElement ->
                        val msgObj = msgElement.jsonObject
                        val msgId =
                            msgObj["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
                        val isOutgoing =
                            msgObj["is_outgoing"]?.jsonPrimitive?.booleanOrNull ?: false

                        val contentObj = msgObj["content"]?.jsonObject
                        // Пытаемся достать текст (если это картинка/стикер, текста может не быть)
                        val text =
                            contentObj?.get("text")?.jsonObject?.get("text")?.jsonPrimitive?.content
                                ?: "[Медиа или сервисное сообщение]"

                        Message(
                            id = msgId,
                            chatId = chatId,
                            senderName = if (isOutgoing) "Вы" else "Собеседник",
                            text = text,
                            isOutgoing = isOutgoing
                        )
                    }

                    _messagesMap.update { current ->
                        current + (chatId to parsedMessages.reversed())
                    }
                }
            }
        } catch (e: Exception) {
        }
    }


    override fun observeChats(): Flow<List<Chat>> {
        tdlibClient.send("""{"@type": "loadChats", "chat_list": {"@type": "chatListMain"}, "limit": 30}""")
        return _chatsMap.map { it.values.toList() }
    }

    override fun observeMessages(chatId: Long): Flow<List<Message>> {
        // 💥 Запрашиваем последние 50 сообщений чата у Telegram
        val request = """
            {
                "@type": "getChatHistory",
                "chat_id": $chatId,
                "from_message_id": 0,
                "offset": 0,
                "limit": 50,
                "only_local": false
            }
        """.trimIndent()
        tdlibClient.send(request)

        return _messagesMap.map { it[chatId] ?: emptyList() }
    }

    override suspend fun sendMessage(chatId: Long, text: String) {
        // 💥 Отправляем текстовое сообщение в C++ ядро TDLib!
        val request = """
            {
                "@type": "sendMessage",
                "chat_id": $chatId,
                "input_message_content": {
                    "@type": "inputMessageText",
                    "text": {
                        "@type": "formattedText",
                        "text": "$text"
                    }
                }
            }
        """.trimIndent()

        tdlibClient.send(request)
    }

    override suspend fun getChatHistory(chatId: Long, limit: Int): String {
        val messages = _messagesMap.value[chatId] ?: emptyList()

        if (messages.isEmpty()) return "История чата пуста."

        // Берем последние N сообщений и склеиваем их в текст для нейросети
        return messages
            .takeLast(limit)
            .joinToString("\n") { message ->
                "${message.senderName}: ${message.text}"
            }
    }
}
