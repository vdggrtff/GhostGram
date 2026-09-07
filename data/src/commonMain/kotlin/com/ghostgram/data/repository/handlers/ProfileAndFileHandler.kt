package com.ghostgram.data.repository.handlers

import com.ghostgram.core.database.dao.MessageDao
import com.ghostgram.core.tdlib.TelegramFlowClient
import entity.Chat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.to

class ProfileAndFileHandler(
    private val myAvatarPath: MutableStateFlow<String?>,
    private val chatsMap: MutableStateFlow<Map<Long, Chat>>,
    private val tdlibClient: TelegramFlowClient,
    private val messageDao: MessageDao,
    private val repoScope: CoroutineScope,
    private val tracker: DownloadTracker // 💥 Наш трекер
) : TdlibUpdateHandler {

    override fun handle(type: String, jsonObject: JsonObject): Boolean {

        // 💥 1. ПЕРЕХВАТ ТВОЕГО ПРОФИЛЯ
        val extra = jsonObject["@extra"]?.jsonPrimitive?.content
        if (extra == "get_me_avatar" && type == "user") {
            val firstName = jsonObject["first_name"]?.jsonPrimitive?.content ?: "Я"
            val photoObj = jsonObject["profile_photo"]?.jsonObject
            val smallPhoto = photoObj?.get("small")?.jsonObject
            val fileId = smallPhoto?.get("id")?.jsonPrimitive?.intOrNull
            val path = smallPhoto?.get("local")?.jsonObject?.get("path")?.jsonPrimitive?.content

            if (!path.isNullOrBlank()) {
                myAvatarPath.value = path
            } else if (fileId != null && fileId != 0) {
                tracker.myAvatarFileId = fileId // Запомнили ID
                tdlibClient.send("""{"@type": "downloadFile", "file_id": $fileId, "priority": 1, "offset": 0, "limit": 0, "synchronous": false}""")
            } else {
                myAvatarPath.value = "INITIALS:$firstName"
            }
            return true
        }

        // 💥 2. ФАЙЛ СКАЧАЛСЯ
        if (type == "updateFile") {
            val fileObj = jsonObject["file"]?.jsonObject ?: return true
            val fileId = fileObj["id"]?.jsonPrimitive?.intOrNull ?: return true
            val localObj = fileObj["local"]?.jsonObject ?: return true
            val isCompleted = localObj["is_downloading_completed"]?.jsonPrimitive?.booleanOrNull ?: false
            val path = localObj["path"]?.jsonPrimitive?.content ?: ""

            if (isCompleted && path.isNotBlank()) {

                // А) Это твоя аватарка?
                if (fileId == tracker.myAvatarFileId) {
                    myAvatarPath.value = path
                }

                // Б) Это аватарка чата?
                tracker.chatAvatars.remove(fileId)?.let { chatId ->
                    chatsMap.update { current ->
                        val chat = current[chatId]
                        if (chat != null) current + (chatId to chat.copy(avatarPath = path)) else current
                    }
                }

                // В) Это фотка в сообщении?
                tracker.messagePhotos.remove(fileId)?.let { messageId ->
                    repoScope.launch {
                        messageDao.updateMessagePhoto(messageId, path) // 💥 Обновляем в SQLite!
                    }
                }
            }
            return true
        }

        return false
    }
}