package com.ghostgram.data.repository.handlers

import entity.Chat
import entity.PublicChat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

class SearchUpdateHandler(
    private val searchResults: MutableStateFlow<List<PublicChat>>,
    private val chatsMap: MutableStateFlow<Map<Long, Chat>>
) : TdlibUpdateHandler {

    override fun handle(type: String, jsonObject: JsonObject): Boolean {
        val extra = jsonObject["@extra"]?.jsonPrimitive?.content

        // 💥 ЛОВИМ ОТВЕТ ПО МЕТКЕ
        if (extra == "search_public" && type == "chats") {
            val chatIds = jsonObject["chat_ids"]?.jsonArray ?: return true
            println("✅ [ПОИСК] TDLib вернул ${chatIds.size} результатов!")

            val chatsList = mutableListOf<PublicChat>()

            chatIds.forEach { elem ->
                val id = elem.jsonPrimitive.longOrNull ?: return@forEach
                val chat = chatsMap.value[id]

                if (chat != null) {
                    if (!AntiSpamFilter.isSpam(chat.title, "public")) {
                        chatsList.add(PublicChat(id = chat.id, title = chat.title, username = "Публичный канал / Группа",   avatarPath = chat.avatarPath))
                    }
                } else {
                    // Если чата еще нет в кэше
                    chatsList.add(PublicChat(id = id, title = "Загрузка...", username = "Публичный канал / Группа", avatarPath = null))
                }
            }

            searchResults.value = chatsList
            return true
        }

        return false
    }
}
