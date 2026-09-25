package com.ghostgram.data.repository.handlers

import com.ghostgram.core.crypto.CryptoLayer
import com.ghostgram.core.crypto.toHex
import com.ghostgram.core.database.dao.MessageDao
import com.ghostgram.core.database.entity.MessageEntity
import com.ghostgram.core.tdlib.TelegramFlowClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

class MessageUpdateHandler(
    private val messageDao: MessageDao,
    private val repoScope: CoroutineScope,
    private val tdlibClient: TelegramFlowClient,          // Нужен для скачивания картинок
    private val lastReadOutboxMap: MutableMap<Long, Long>,
    private val tracker: DownloadTracker,
    private val cryptoLayer: CryptoLayer,
) : TdlibUpdateHandler {

    private val usersCache = mutableMapOf<Long, Pair<String, String?>>()

    override fun handle(type: String, jsonObject: JsonObject): Boolean {
        when (type) {
            // 1. ПРИШЛО НОВОЕ СООБЩЕНИЕ
            "updateNewMessage" -> {
                val messageObj = jsonObject["message"]?.jsonObject ?: return true
                val chatId = messageObj["chat_id"]?.jsonPrimitive?.longOrNull ?: return true
                val messageId = messageObj["id"]?.jsonPrimitive?.longOrNull ?: return true
                val isOutgoing = messageObj["is_outgoing"]?.jsonPrimitive?.booleanOrNull ?: false
                val date = messageObj["date"]?.jsonPrimitive?.intOrNull ?: 0
                val mediaAlbumId = messageObj["media_album_id"]?.jsonPrimitive?.longOrNull ?: 0L
                val isSending = messageObj["sending_state"] != null
                val replyToObj = messageObj["reply_to"]?.jsonObject
                val replyToMessageId = if (replyToObj?.get("@type")?.jsonPrimitive?.content == "messageReplyToMessage") {
                    replyToObj["message_id"]?.jsonPrimitive?.longOrNull ?: 0L
                } else 0L
                val editDate = messageObj["edit_date"]?.jsonPrimitive?.intOrNull ?: 0
                val isEdited = editDate > 0
                val senderObj = messageObj["sender_id"]?.jsonObject
                val senderId = senderObj?.get("user_id")?.jsonPrimitive?.longOrNull
                    ?: senderObj?.get("chat_id")?.jsonPrimitive?.longOrNull
                    ?: 0L

                val contentObj = messageObj["content"]?.jsonObject

                // Парсим сообщение в сущность базы данных
                val entity =
                    parseSingleMessageToEntity(
                        messageId,
                        chatId,
                        isOutgoing,
                        contentObj,
                        date,
                        mediaAlbumId,
                        isLive = true,
                        isSending = isSending,
                        replyToMessageId = replyToMessageId,
                        isEdited = isEdited,
                        senderId = senderId
                    )

                if (entity != null) {
                    repoScope.launch {
                        messageDao.insertMessage(entity)
                    }
                }
                return true
            }
            // 2. ПРИЛЕТЕЛА ИСТОРИЯ ЧАТА (ПАЧКА СООБЩЕНИЙ)
            "messages" -> {
                val messagesArray = jsonObject["messages"]?.jsonArray ?: return true
                if (messagesArray.isEmpty()) return true
                val chatId =
                    messagesArray[0].jsonObject["chat_id"]?.jsonPrimitive?.longOrNull ?: return true

                // Парсим весь массив
                val parsedEntities = messagesArray.reversed().mapNotNull { msgElement ->
                    val msgObj = msgElement.jsonObject
                    val msgId = msgObj["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
                    val isOutgoing = msgObj["is_outgoing"]?.jsonPrimitive?.booleanOrNull ?: false
                    val isSending = msgObj["sending_state"] != null
                    val replyToObj = msgObj["reply_to"]?.jsonObject
                    val replyToMessageId = if (replyToObj?.get("@type")?.jsonPrimitive?.content == "messageReplyToMessage") {
                        replyToObj["message_id"]?.jsonPrimitive?.longOrNull ?: 0L
                    } else 0L
                    val contentObj = msgObj["content"]?.jsonObject

                    val date = msgObj["date"]?.jsonPrimitive?.intOrNull ?: 0
                    // ДОСТАЕМ АЛЬБОМ ИЗ ИСТОРИИ!
                    val mediaAlbumId = msgObj["media_album_id"]?.jsonPrimitive?.longOrNull ?: 0L
                    val editDate = msgObj["edit_date"]?.jsonPrimitive?.intOrNull ?: 0
                    val isEdited = editDate > 0
                    val senderObj = msgObj["sender_id"]?.jsonObject
                    val senderId = senderObj?.get("user_id")?.jsonPrimitive?.longOrNull
                        ?: senderObj?.get("chat_id")?.jsonPrimitive?.longOrNull
                        ?: 0L

                    parseSingleMessageToEntity(
                        msgId,
                        chatId,
                        isOutgoing,
                        contentObj,
                        date,
                        mediaAlbumId,
                        isLive = false,
                        isSending = isSending,
                        replyToMessageId = replyToMessageId,
                        isEdited = isEdited,
                        senderId = senderId
                    )
                }

                repoScope.launch {
                    // Сохраняем пачку в базу
                    messageDao.insertMessages(parsedEntities)

                    // Восстанавливаем синие галочки ✓✓
                    val lastReadId = lastReadOutboxMap[chatId] ?: 0L
                    if (lastReadId > 0) {
                        messageDao.markOutboxAsRead(chatId, lastReadId)
                    }
                }
                return true
            }
            // 3. УБИВАЕМ ФАНТОМА (Сообщение успешно доставлено на сервер)
            "updateMessageSendSucceeded" -> {
                val oldId = jsonObject["old_message_id"]?.jsonPrimitive?.longOrNull ?: return true
                val messageObj = jsonObject["message"]?.jsonObject ?: return true
                val chatId = messageObj["chat_id"]?.jsonPrimitive?.longOrNull ?: return true
                val newId = messageObj["id"]?.jsonPrimitive?.longOrNull ?: return true
                val date = messageObj["date"]?.jsonPrimitive?.intOrNull ?: 0
                val mediaAlbumId = messageObj["media_album_id"]?.jsonPrimitive?.longOrNull ?: 0L
                val contentObj = messageObj["content"]?.jsonObject

                val entity = parseSingleMessageToEntity(
                    newId,
                    chatId,
                    true,
                    contentObj,
                    date = date,
                    mediaAlbumId = mediaAlbumId,
                    isLive = false,
                    isSending = false
                )

                repoScope.launch {
                    messageDao.deleteMessage(
                        chatId = chatId,
                        messageId = oldId
                    ) // Удалили старое временное
                    if (entity != null) {
                        messageDao.insertMessage(entity) // Записали новое настоящее
                    }
                }
                return true
            }
            // 5. ANTI-REVOKE (Собеседник удалил сообщение)
            "updateDeleteMessages" -> {
                val chatId = jsonObject["chat_id"]?.jsonPrimitive?.longOrNull ?: return true
                val messageIds =
                    jsonObject["message_ids"]?.jsonArray?.mapNotNull { it.jsonPrimitive.longOrNull }
                        ?: emptyList()

                repoScope.launch {
                    messageIds.forEach { msgId ->
                        messageDao.markAsDeleted(chatId = chatId, messageId = msgId)
                    }
                }
                return true
            }
            "updateMessageContent" -> {
                val chatId = jsonObject["chat_id"]?.jsonPrimitive?.longOrNull ?: return true
                val msgId = jsonObject["message_id"]?.jsonPrimitive?.longOrNull ?: return true
                val newContent = jsonObject["new_content"]?.jsonObject ?: return true

                if (newContent["@type"]?.jsonPrimitive?.content == "messageText") {
                    var text = newContent["text"]?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""
                    var extraInfo: String? = null

                    // РАСШИФРОВЫВАЕМ НОВЫЙ ТЕКСТ, ЕСЛИ ЭТО КРИПТА!
                    // (Предполагается, что в MessageUpdateHandler у тебя прокинут cryptoLayer)
                    if (text.contains("👻 ")) {
                        val decrypted = cryptoLayer.revealAndDecrypt(chatId, text)
                        if (decrypted != null) {
                            text = decrypted
                            extraInfo = "ENCRYPTED"
                        }
                    }

                    repoScope.launch {
                        messageDao.updateMessageText(msgId, text, extraInfo)
                    }
                }
                return true
            }

            // 2. ТЕЛЕГРАМ СКАЗАЛ, ЧТО СООБЩЕНИЕ ИЗМЕНЕНО
            "updateMessageEdited" -> {
                val msgId = jsonObject["message_id"]?.jsonPrimitive?.longOrNull ?: return true
                repoScope.launch {
                    messageDao.markMessageAsEdited(msgId)
                }
                return true
            }
            "user", "updateUser" -> {
                if (jsonObject["@extra"]?.jsonPrimitive?.content == "get_me_avatar") return false
                val userObj = if (type == "updateUser") jsonObject["user"]?.jsonObject else jsonObject
                val userId = userObj?.get("id")?.jsonPrimitive?.longOrNull ?: return true
                val firstName = userObj["first_name"]?.jsonPrimitive?.content ?: ""
                val lastName = userObj["last_name"]?.jsonPrimitive?.content ?: ""
                val fullName = "$firstName $lastName".trim().ifBlank { "Участник" }

                val photoObj = userObj["profile_photo"]?.jsonObject
                val smallPhoto = photoObj?.get("small")?.jsonObject
                val fileId = smallPhoto?.get("id")?.jsonPrimitive?.intOrNull
                val avatarPath = smallPhoto?.get("local")?.jsonObject?.get("path")?.jsonPrimitive?.content

                // Если фото нет на диске — качаем
                if (avatarPath.isNullOrBlank() && fileId != null && fileId != 0) {
                    tracker.chatAvatars[fileId] = userId // Юзаем трекер
                    tdlibClient.send("""{"@type": "downloadFile", "file_id": $fileId, "priority": 16, "offset": 0, "limit": 0, "synchronous": false}""")
                }

                val finalPath = if (!avatarPath.isNullOrBlank()) avatarPath else null
                usersCache[userId] = fullName to finalPath

                // Обновляем все старые сообщения этого человека в базе данных!
                repoScope.launch {
                    messageDao.updateSenderInfo(userId, fullName, finalPath)
                }
                return true
            }
        }
        return false // Если тип другой — мы его не трогаем
    }

    private fun parseSingleMessageToEntity(
        msgId: Long,
        chatId: Long,
        isOutgoing: Boolean,
        contentObj: JsonObject?,
        date: Int = 0,
        mediaAlbumId: Long = 0L,
        isLive: Boolean = false,
        isSending: Boolean = false,
        replyToMessageId: Long = 0L,
        isEdited: Boolean = false,
        senderId: Long = 0L
    ): MessageEntity? {
        val contentType = contentObj?.get("@type")?.jsonPrimitive?.content ?: return null
        val senderName = if (isOutgoing) "Вы" else "Собеседник"

        var realSenderName = "Собеседник"
        var realSenderAvatar: String? = null

        if (isOutgoing) {
            realSenderName = "Вы"
        } else if (senderId != 0L) {
            val cachedUser = usersCache[senderId]
            if (cachedUser != null) {
                realSenderName = cachedUser.first
                realSenderAvatar = cachedUser.second
            } else {
                tdlibClient.send("""{"@type": "getUser", "user_id": $senderId}""")
                realSenderName = "Участник"
            }
        }

        return when (contentType) {
            "messageText" -> {
                var text = contentObj["text"]?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""
                var extraInfo: String? = null

                // 1. КТО-ТО ПРЕДЛАГАЕТ НАМ ОБМЕН КЛЮЧАМИ
                if (text.startsWith("👻🔑 ")) {
                    // ТОЛЬКО ДЛЯ ЧУЖИХ СООБЩЕНИЙ СОХРАНЯЕМ СЕКРЕТ! (чтобы не сломать ключ о самого себя)
                    if (!isOutgoing) {
                        val otherPubKeyHex = text.removePrefix("👻🔑 ")
                        cryptoLayer.establishSecret(chatId, otherPubKeyHex)

                        // Отвечаем ТОЛЬКО если это новое сообщение (isLive == true), а не из истории!
                        if (isLive) {
                            val myPubKeyHex = cryptoLayer.myKeyPair.second.toHex()
                            tdlibClient.send(
                                """
                                {
                                    "@type": "sendMessage", 
                                    "chat_id": $chatId, 
                                    "input_message_content": {
                                        "@type": "inputMessageText", 
                                        "text": {"@type": "formattedText", "text": "👻🤝 $myPubKeyHex"},
                                        "link_preview_options": {"@type": "linkPreviewOptions", "is_disabled": true}
                                    }
                                }
                            """.trimIndent()
                            )
                        }
                    }
                    text = "🔐 Запрос E2EE отправлен..."
                    extraInfo = "SYSTEM"
                }
                // 2. СОБЕСЕДНИК ПОДТВЕРДИЛ ОБМЕН
                else if (text.startsWith("👻🤝 ")) {
                    // ТОЛЬКО ДЛЯ ЧУЖИХ СООБЩЕНИЙ!
                    if (!isOutgoing) {
                        val otherPubKeyHex = text.removePrefix("👻🤝 ")
                        cryptoLayer.establishSecret(chatId, otherPubKeyHex)
                    }
                    text = "✅ Защищенный E2EE канал установлен!"
                    extraInfo = "SYSTEM"
                }
                // 3. РАСШИФРОВКА ТЕКСТА
                else if (text.contains("👻 ")) {
                    val decrypted = cryptoLayer.revealAndDecrypt(chatId, text)
                    if (decrypted != null) {
                        text = decrypted
                        extraInfo = "ENCRYPTED" // Ставим метку для зеленого замка в UI
                    } else {
                        text = "❌ Ошибка дешифровки E2EE\n$text"
                        extraInfo = "SYSTEM"
                    }
                }

                MessageEntity(
                    id = msgId,
                    chatId = chatId,
                    senderName = realSenderName,
                    text = text,
                    isOutgoing = isOutgoing,
                    mediaType = "TEXT",
                    fileExtraInfo = extraInfo,
                    date = date,
                    mediaAlbumId = mediaAlbumId,
                    isSending = isSending,
                    replyToMessageId = replyToMessageId,
                    isEdited = isEdited,
                    senderId = senderId,
                    senderAvatarPath = realSenderAvatar
                )
            }

            "messagePhoto" -> {
                val caption =
                    contentObj["caption"]?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""
                val photoSizes = contentObj["photo"]?.jsonObject?.get("sizes")?.jsonArray
                val largestPhoto = photoSizes?.lastOrNull()?.jsonObject?.get("photo")?.jsonObject
                val fileId = largestPhoto?.get("id")?.jsonPrimitive?.intOrNull
                val photoPath =
                    largestPhoto?.get("local")?.jsonObject?.get("path")?.jsonPrimitive?.content

                // Просим TDLib скачать фотку, если её нет на диске
                if (photoPath.isNullOrBlank() && fileId != null && fileId != 0) {
                    tracker.messagePhotos[fileId] = msgId // Записали в трекер!
                    tdlibClient.send("""{"@type": "downloadFile", "file_id": $fileId, "priority": 1, "offset": 0, "limit": 0, "synchronous": false}""")
                }

                MessageEntity(
                    id = msgId,
                    chatId = chatId,
                    senderName = realSenderName,
                    text = caption,
                    isOutgoing = isOutgoing,
                    photoPath = photoPath,
                    mediaType = "PHOTO",
                    date = date,
                    mediaAlbumId = mediaAlbumId,
                    isSending = isSending,
                    replyToMessageId = replyToMessageId,
                    isEdited = isEdited,
                    senderId = senderId,
                    senderAvatarPath = realSenderAvatar
                )
            }

            "messageDocument" -> {
                val docObj = contentObj["document"]?.jsonObject
                val fileName = docObj?.get("file_name")?.jsonPrimitive?.content ?: "Файл"
                val caption =
                    contentObj["caption"]?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""
                MessageEntity(
                    id = msgId,
                    chatId = chatId,
                    senderName = realSenderName,
                    text = caption,
                    isOutgoing = isOutgoing,
                    mediaType = "DOCUMENT",
                    fileName = fileName,
                    date = date,
                    mediaAlbumId = mediaAlbumId,
                    isSending = isSending,
                    replyToMessageId = replyToMessageId,
                    isEdited = isEdited,
                    senderId = senderId,
                    senderAvatarPath = realSenderAvatar
                )
            }

            "messageVoiceNote" -> {
                val voiceObj = contentObj["voice_note"]?.jsonObject
                val duration = voiceObj?.get("duration")?.jsonPrimitive?.intOrNull ?: 0
                val caption = contentObj["caption"]?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""

                // ДОСТАЕМ АУДИОФАЙЛ (.ogg)
                val fileObj = voiceObj?.get("voice")?.jsonObject
                val fileId = fileObj?.get("id")?.jsonPrimitive?.intOrNull
                val filePath = fileObj?.get("local")?.jsonObject?.get("path")?.jsonPrimitive?.content

                // Если файла нет на диске — качаем на максимальной скорости!
                if (filePath.isNullOrBlank() && fileId != null && fileId != 0) {
                    tracker.messageFiles[fileId] = msgId // Записываем в трекер файлов!
                    tdlibClient.send("""{"@type": "downloadFile", "file_id": $fileId, "priority": 32, "offset": 0, "limit": 0, "synchronous": false}""")
                }
                MessageEntity(
                    id = msgId,
                    chatId = chatId,
                    senderName = realSenderName,
                    text = caption,
                    isOutgoing = isOutgoing,
                    mediaType = "VOICE",
                    fileExtraInfo = formatDuration(duration),
                    fileName = filePath, // СОХРАНЯЕМ ПУТЬ К ЗВУКУ!
                    date = date,
                    mediaAlbumId = mediaAlbumId,
                    isSending = isSending,
                    replyToMessageId = replyToMessageId,
                    isEdited = isEdited,
                    senderId = senderId,
                    senderAvatarPath = realSenderAvatar
                )
            }

            "messageVideo", "messageVideoNote" -> {
                val videoObj =
                    contentObj[if (contentType == "messageVideo") "video" else "video_note"]?.jsonObject
                val duration = videoObj?.get("duration")?.jsonPrimitive?.intOrNull ?: 0
                val caption =
                    contentObj["caption"]?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""

                // ДОСТАЕМ КАРТИНКУ-ПРЕВЬЮШКУ ВИДЕО!
                val thumbObj = videoObj?.get("thumbnail")?.jsonObject?.get("file")?.jsonObject
                val fileId = thumbObj?.get("id")?.jsonPrimitive?.intOrNull
                val photoPath =
                    thumbObj?.get("local")?.jsonObject?.get("path")?.jsonPrimitive?.content

                if (photoPath.isNullOrBlank() && fileId != null && fileId != 0) {
                    tracker.messagePhotos[fileId] = msgId
                    tdlibClient.send("""{"@type": "downloadFile", "file_id": $fileId, "priority": 1, "offset": 0, "limit": 0, "synchronous": false}""")
                }

                val mainVideoObj = videoObj?.get("video")?.jsonObject
                val videoFileId = mainVideoObj?.get("id")?.jsonPrimitive?.intOrNull
                val videoPath = mainVideoObj?.get("local")?.jsonObject?.get("path")?.jsonPrimitive?.content

                if (videoPath.isNullOrBlank() && videoFileId != null && videoFileId != 0) {
                    tracker.messageFiles[videoFileId] = msgId // КЛАДЕМ В FILES!
                    tdlibClient.send("""{"@type": "downloadFile", "file_id": $videoFileId, "priority": 1, "offset": 0, "limit": 0, "synchronous": false}""")
                }

                MessageEntity(
                    id = msgId,
                    chatId = chatId,
                    senderName = realSenderName,
                    text = caption,
                    isOutgoing = isOutgoing,
                    mediaType = if (contentType == "messageVideo") "VIDEO" else "VIDEO_NOTE",
                    fileExtraInfo = formatDuration(duration),
                    photoPath = photoPath, // Сохраняем путь к превьюшке!
                    date = date,
                    mediaAlbumId = mediaAlbumId,
                    fileName = videoPath,
                    isSending = isSending,
                    replyToMessageId = replyToMessageId,
                    isEdited = isEdited,
                    senderId = senderId,
                    senderAvatarPath = realSenderAvatar
                )
            }

            "messageSticker" -> {
                val stickerObj = contentObj["sticker"]?.jsonObject
                val emoji = stickerObj?.get("emoji")?.jsonPrimitive?.content ?: "✨"

                // КАЧАЕМ ПОЛНОЦЕННЫЙ СТИКЕР (не thumbnail!)
                val fileObj = stickerObj?.get("sticker")?.jsonObject
                val fileId = fileObj?.get("id")?.jsonPrimitive?.intOrNull
                val stickerPath =
                    fileObj?.get("local")?.jsonObject?.get("path")?.jsonPrimitive?.content

                if (stickerPath.isNullOrBlank() && fileId != null && fileId != 0) {
                    tracker.messagePhotos[fileId] = msgId
                    tdlibClient.send("""{"@type": "downloadFile", "file_id": $fileId, "priority": 1, "offset": 0, "limit": 0, "synchronous": false}""")
                }

                MessageEntity(
                    id = msgId, chatId = chatId, senderName = realSenderName, text = "",
                    isOutgoing = isOutgoing, mediaType = "STICKER", fileExtraInfo = emoji,
                    photoPath = stickerPath, date = date,
                    isSending = isSending,
                    replyToMessageId = replyToMessageId,
                    isEdited = isEdited,
                    senderId = senderId,
                    senderAvatarPath = realSenderAvatar
                )
            }

            else -> {
                MessageEntity(
                    id = msgId,
                    chatId = chatId,
                    senderName = realSenderName,
                    text = "[Медиа]",
                    isOutgoing = isOutgoing,
                    mediaType = "TEXT",
                    date = date,
                    mediaAlbumId = mediaAlbumId,
                    isSending = isSending,
                    replyToMessageId = replyToMessageId,
                    isEdited = isEdited,
                    senderId = senderId,
                    senderAvatarPath = realSenderAvatar
                )
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val min = seconds / 60
    val sec = seconds % 60
    // Если секунд меньше 10, добавляем нолик спереди (чтобы было 1:05, а не 1:5)
    return "$min:${if (sec < 10) "0$sec" else "$sec"}"
}

