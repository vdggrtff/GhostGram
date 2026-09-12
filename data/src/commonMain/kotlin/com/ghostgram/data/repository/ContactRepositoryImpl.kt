package com.ghostgram.data.repository

import com.ghostgram.core.tdlib.TelegramFlowClient
import entity.Contact
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
import repository.ContactRepository

class ContactRepositoryImpl(
    private val tdlibClient: TelegramFlowClient
) : ContactRepository {

    private val jsonParser = Json { ignoreUnknownKeys = true }
    private val _contactsMap = MutableStateFlow<Map<Long, Contact>>(emptyMap())
    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        tdlibClient.updates
            .onEach { rawJson -> parseUpdate(rawJson) }
            .launchIn(scope)
    }

    private fun parseUpdate(rawJson: String) {
        try {
            val jsonObject = jsonParser.parseToJsonElement(rawJson).jsonObject
            val type = jsonObject["@type"]?.jsonPrimitive?.content ?: return

            when (type) {
                // 💥 1. Список ID всех контактов
                "users" -> {
                    val userIds = jsonObject["user_ids"]?.jsonArray ?: return
                    userIds.forEach { idElem ->
                        val userId = idElem.jsonPrimitive.longOrNull ?: return@forEach
                        // Для каждого ID просим у Telegram подробную информацию о юзере
                        tdlibClient.send("""{"@type": "getUser", "user_id": $userId}""")
                    }
                }

                // 💥 2. Информация о конкретном юзере (Имя, юзернейм, аватарка, статус)
                "user", "updateUser" -> {
                    val userObj = if (type == "updateUser") jsonObject["user"]?.jsonObject else jsonObject
                    val id = userObj?.get("id")?.jsonPrimitive?.longOrNull ?: return
                    val firstName = userObj["first_name"]?.jsonPrimitive?.content ?: ""
                    val lastName = userObj["last_name"]?.jsonPrimitive?.content ?: ""
                    val phoneNumber = userObj["phone_number"]?.jsonPrimitive?.content ?: ""

                    // Юзернейм
                    val username = userObj["usernames"]?.jsonObject?.get("editable_username")?.jsonPrimitive?.content

                    // Статус онлайна
                    val statusObj = userObj["status"]?.jsonObject
                    val statusType = statusObj?.get("@type")?.jsonPrimitive?.content ?: ""
                    val isOnline = statusType == "userStatusOnline"
                    val statusText = when (statusType) {
                        "userStatusOnline" -> "в сети"
                        "userStatusRecently" -> "был(а) недавно"
                        "userStatusLastWeek" -> "был(а) на этой неделе"
                        else -> "не в сети"
                    }

                    // Аватарка
                    val smallPhoto = userObj["profile_photo"]?.jsonObject?.get("small")?.jsonObject
                    val fileId = smallPhoto?.get("id")?.jsonPrimitive?.intOrNull
                    val avatarPath = smallPhoto?.get("local")?.jsonObject?.get("path")?.jsonPrimitive?.content

                    if (avatarPath.isNullOrBlank() && fileId != null && fileId != 0) {
                        tdlibClient.send("""{"@type": "downloadFile", "file_id": $fileId, "priority": 1, "offset": 0, "limit": 0, "synchronous": false}""")
                    }

                    val contact = Contact(
                        userId = id,
                        firstName = firstName,
                        lastName = lastName,
                        username = username,
                        phoneNumber = phoneNumber,
                        avatarPath = if (!avatarPath.isNullOrBlank()) avatarPath else null,
                        isOnline = isOnline,
                        statusText = statusText
                    )

                    _contactsMap.update { it + (id to contact) }
                }

                // Скачалась аватарка
                "updateFile" -> {
                    val fileObj = jsonObject["file"]?.jsonObject ?: return
                    val fileId = fileObj["id"]?.jsonPrimitive?.intOrNull ?: return
                    val localObj = fileObj["local"]?.jsonObject ?: return
                    val isCompleted = localObj["is_downloading_completed"]?.jsonPrimitive?.booleanOrNull ?: false
                    val path = localObj["path"]?.jsonPrimitive?.content ?: ""

                    if (isCompleted && path.isNotBlank()) {
                        _contactsMap.update { map ->
                            map.mapValues { (_, contact) ->
                                if (contact.avatarPath == null) contact.copy(avatarPath = path) else contact
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) { }
    }

    override fun observeContacts(): Flow<List<Contact>> {
        // 💥 Просим у Telegram список контактов
        tdlibClient.send("""{"@type": "getContacts"}""")
        return _contactsMap.map { it.values.toList() }
    }

    override suspend fun createPrivateChat(userId: Long): Long {
        // В Telegram ID приватного чата с человеком равен его userId!
        tdlibClient.send("""{"@type": "createPrivateChat", "user_id": $userId, "force": false}""")
        return userId
    }
}