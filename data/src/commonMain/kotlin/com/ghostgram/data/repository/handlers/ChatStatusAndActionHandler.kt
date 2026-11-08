package com.ghostgram.data.repository.handlers

import entity.UserOnlineStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

class ChatStatusAndActionHandler(
    private val scope: CoroutineScope
) : TdlibUpdateHandler {

    private val _userStatusMap = MutableStateFlow<Map<Long, UserOnlineStatus>>(emptyMap())
    private val _typingMap = MutableStateFlow<Map<Long, String?>>(emptyMap())
    private val typingResetJobs = mutableMapOf<Long, Job>()

    fun observeUserStatus(userId: Long): Flow<UserOnlineStatus?> =
        _userStatusMap.map { it[userId] }

    fun observeChatTyping(chatId: Long): Flow<String?> =
        _typingMap.map { it[chatId] }

    override fun handle(type: String, jsonObject: JsonObject): Boolean {
        when (type) {
            "user", "updateUser" -> {
                val userObj = if (type == "user") jsonObject else jsonObject["user"]?.jsonObject
                val userId = userObj?.get("id")?.jsonPrimitive?.longOrNull ?: return false
                val statusObj = userObj["status"]?.jsonObject
                _userStatusMap.update { it + (userId to parseUserStatus(statusObj)) }
                return false
            }

            "updateUserStatus" -> {
                val userId = jsonObject["user_id"]?.jsonPrimitive?.longOrNull ?: return false
                val statusObj = jsonObject["status"]?.jsonObject
                _userStatusMap.update { it + (userId to parseUserStatus(statusObj)) }
                return true
            }

            "updateChatAction", "updateUserChatAction" -> {
                val chatId = jsonObject["chat_id"]?.jsonPrimitive?.longOrNull ?: return false
                val actionObj = jsonObject["action"]?.jsonObject
                val actionType = actionObj?.get("@type")?.jsonPrimitive?.content ?: ""

                val actionText = when (actionType) {
                    "chatActionTyping" -> "печатает..."
                    "chatActionRecordingVoiceNote" -> "записывает голосовое..."
                    "chatActionRecordingVideoNote" -> "записывает видео..."
                    "chatActionUploadingPhoto" -> "отправляет фото..."
                    "chatActionUploadingVideo" -> "отправляет видео..."
                    "chatActionUploadingDocument" -> "отправляет файл..."
                    "chatActionChoosingSticker" -> "выбирает стикер..."
                    "chatActionCancel" -> null
                    else -> null
                }

                _typingMap.update { it + (chatId to actionText) }

                typingResetJobs[chatId]?.cancel()
                if (actionText != null) {
                    typingResetJobs[chatId] = scope.launch {
                        delay(4500)
                        _typingMap.update { it + (chatId to null) }
                    }
                }
                return true
            }
        }
        return false
    }

    private fun parseUserStatus(statusObj: JsonObject?): UserOnlineStatus {
        val type = statusObj?.get("@type")?.jsonPrimitive?.content ?: ""
        val isOnline = type == "userStatusOnline"
        val wasOnline = statusObj?.get("was_online")?.jsonPrimitive?.intOrNull ?: 0
        return UserOnlineStatus(isOnline = isOnline, wasOnline = wasOnline, statusType = type)
    }
}