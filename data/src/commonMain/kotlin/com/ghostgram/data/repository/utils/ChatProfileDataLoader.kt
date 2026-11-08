package com.ghostgram.data.repository.utils

import com.ghostgram.core.tdlib.TelegramFlowClient
import com.ghostgram.core.tdlib.sendAndAwait
import entity.ChatFullProfile
import entity.Message
import entity.MessageMediaType
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

class ChatProfileDataLoader(
    private val tdlibClient: TelegramFlowClient,
    private val messageParser: TdlibMessageParser,
    private val chatCache: ChatMemoryCache
) {
    suspend fun getFullProfile(chatId: Long): ChatFullProfile? {
        val chat = chatCache.getChat(chatId)

        if (chatId > 0) {
            val userObj = tdlibClient.sendAndAwait("getUser", mapOf("user_id" to chatId))
            val fullInfoObj = tdlibClient.sendAndAwait("getUserFullInfo", mapOf("user_id" to chatId))

            val bio = fullInfoObj?.get("bio")?.jsonObject?.get("text")?.jsonPrimitive?.content
                ?: fullInfoObj?.get("bio")?.jsonPrimitive?.content ?: ""
            val phone = userObj?.get("phone_number")?.jsonPrimitive?.content ?: ""
            val username = userObj?.get("usernames")?.jsonObject?.get("editable_username")?.jsonPrimitive?.content
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
                val supergroupFull = tdlibClient.sendAndAwait("getSupergroupFullInfo", mapOf("supergroup_id" to supergroupId))
                memberCount = supergroupFull?.get("member_count")?.jsonPrimitive?.intOrNull ?: 0
                description = supergroupFull?.get("description")?.jsonPrimitive?.content ?: "Описание отсутствует"
            }

            return ChatFullProfile(
                id = chatId, title = title, avatarPath = chat?.avatarPath,
                bio = description, isGroup = true, memberCount = memberCount
            )
        }
    }

    suspend fun getSharedMedia(chatId: Long, filterType: String, fromMessageId: Long): List<Message> {
        val filterObj = buildJsonObject { put("@type", filterType) }

        val response = tdlibClient.sendAndAwait(
            requestType = "searchChatMessages",
            parameters = mapOf(
                "chat_id" to chatId, "query" to "", "filter" to filterObj,
                "from_message_id" to fromMessageId, "offset" to 0, "limit" to 50
            ),
            timeoutMs = 8000
        ) ?: return emptyList()

        val messagesArray = response["messages"]?.jsonArray ?: return emptyList()

        val expectedTypes = when (filterType) {
            "searchMessagesFilterPhotoAndVideo" -> listOf(MessageMediaType.PHOTO, MessageMediaType.VIDEO, MessageMediaType.VIDEO_NOTE)
            "searchMessagesFilterDocument" -> listOf(MessageMediaType.DOCUMENT)
            "searchMessagesFilterVoiceNote" -> listOf(MessageMediaType.VOICE)
            else -> emptyList()
        }

        return messagesArray.mapNotNull { elem ->
            val message = messageParser.parse(elem.jsonObject, fallbackChatId = chatId) ?: return@mapNotNull null
            if (expectedTypes.isNotEmpty() && message.mediaType !in expectedTypes) return@mapNotNull null
            message
        }
    }

    suspend fun getPinnedMessage(chatId: Long): Message? {
        val filterObj = buildJsonObject { put("@type", "searchMessagesFilterPinned") }
        val response = tdlibClient.sendAndAwait(
            requestType = "searchChatMessages",
            parameters = mapOf("chat_id" to chatId, "query" to "", "filter" to filterObj, "from_message_id" to 0L, "offset" to 0, "limit" to 1),
            timeoutMs = 4000
        ) ?: return null

        val firstMsg = response["messages"]?.jsonArray?.firstOrNull()?.jsonObject ?: return null
        return messageParser.parse(firstMsg, fallbackChatId = chatId)
    }
}