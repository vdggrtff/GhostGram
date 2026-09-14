package com.ghostgram.data.repository.handlers

import AntiSpamFilter
import entity.Chat
import entity.Message
import entity.PublicChat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

class SearchUpdateHandler(
    private val searchResults: MutableStateFlow<List<PublicChat>>,
    private val messageSearchResults: MutableStateFlow<List<Chat>>,
    private val chatsMap: MutableStateFlow<Map<Long, Chat>>,
) : TdlibUpdateHandler {

    override fun handle(type: String, jsonObject: JsonObject): Boolean {
        //val extra = jsonObject["@extra"]?.jsonPrimitive?.content
        val extra = jsonObject["@extra"]?.jsonPrimitive?.content

        if (extra?.startsWith("search_msg_") == true) {
            println("📡 [СЫРОЙ ОТВЕТ ПОИСКА] type='$type'")
        }


        // 💥 ЛОВИМ ОТВЕТ ПО МЕТКЕ
        if (extra?.startsWith("search_public") == true && type == "chats") {
            val chatIds = jsonObject["chat_ids"]?.jsonArray ?: return true
            println("✅ [ПОИСК] TDLib вернул ${chatIds.size} результатов!")

            val chatsList = mutableListOf<PublicChat>()

            chatIds.forEach { elem ->
                val id = elem.jsonPrimitive.longOrNull ?: return@forEach
                val chat = chatsMap.value[id]

                if (chat != null) {
                    if (!AntiSpamFilter.isSpam(chat.title, "public")) {
                        chatsList.add(
                            PublicChat(
                                id = chat.id,
                                title = chat.title,
                                username = "Публичный канал / Группа",
                                avatarPath = chat.avatarPath
                            )
                        )
                    }
                } else {
                    // Если чата еще нет в кэше
                    chatsList.add(
                        PublicChat(
                            id = id,
                            title = "Загрузка...",
                            username = "Публичный канал / Группа",
                            avatarPath = null
                        )
                    )
                }
            }

            searchResults.value = chatsList
            return true
        }

        if (extra?.startsWith("search_msg_") == true && type == "error") {
            println("❌ [ПОИСК СООБЩЕНИЙ] ОШИБКА ОТ TDLIB: $jsonObject")
            return true
        }

        if (extra?.startsWith("search_msg_") == true && (type == "messages" || type == "foundMessages")) {
            val messagesArray = jsonObject["messages"]?.jsonArray ?: return true

            println("✅ [ПОИСК СООБЩЕНИЙ] TDLib вернул массив! Найдено: ${messagesArray.size} сообщений")

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

                println("💬 [ПОИСК СООБЩЕНИЙ] Парсим сообщение из чата $chatId. Текст: '$text'")

                val chat = chatsMap.value[chatId]
                val chatTitle = chat?.title ?: "Найденный диалог"
                val chatAvatar = chat?.avatarPath

                val foundMsg = Message(id = msgId, chatId = chatId, senderName = if (isOutgoing) "Вы" else chatTitle, text = text, date = date)

                val resultChat = chat?.copy(lastMessage = foundMsg) ?: Chat(
                    id = chatId, title = chatTitle, unreadCount = 0, lastMessage = foundMsg, avatarPath = chatAvatar
                )

                results.add(resultChat)
            }

            println("✅ [ПОИСК СООБЩЕНИЙ] Итого сформировано чатов для UI: ${results.size}")
            messageSearchResults.value = results
            return true
        }

        return false
    }
}
