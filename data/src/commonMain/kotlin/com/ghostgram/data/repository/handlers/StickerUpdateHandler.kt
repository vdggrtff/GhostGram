package com.ghostgram.data.repository.handlers

import com.ghostgram.core.tdlib.TelegramFlowClient
import com.ghostgram.data.repository.utils.DownloadTracker
import entity.TelegramSticker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class StickerUpdateHandler(
    private val recentStickers: MutableStateFlow<List<TelegramSticker>>,
    private val tdlibClient: TelegramFlowClient,
    private val tracker: DownloadTracker
) : TdlibUpdateHandler {

    override fun handle(type: String, jsonObject: JsonObject): Boolean {
        // ЛОВИМ ОТВЕТ С НЕСТАНДАРТНЫМИ / НЕДАВНИМИ СТИКЕРАМИ
        if (type == "stickers") {
            val stickersArray = jsonObject["stickers"]?.jsonArray ?: return true
            val list = mutableListOf<TelegramSticker>()

            stickersArray.forEach { stickerElement ->
                val stickerObj = stickerElement.jsonObject
                val fileObj = stickerObj["sticker"]?.jsonObject
                val stickerFileId = fileObj?.get("id")?.jsonPrimitive?.intOrNull ?: return@forEach

                // ДОСТАЕМ НАСТОЯЩИЙ ОБЛАЧНЫЙ REMOTE ID!
                val remoteFileId = fileObj["remote"]?.jsonObject?.get("id")?.jsonPrimitive?.content ?: return@forEach

                val emoji = stickerObj["emoji"]?.jsonPrimitive?.content ?: "✨"

                val thumbObj = stickerObj["thumbnail"]?.jsonObject?.get("file")?.jsonObject
                val thumbFileId = thumbObj?.get("id")?.jsonPrimitive?.intOrNull
                val thumbPath = thumbObj?.get("local")?.jsonObject?.get("path")?.jsonPrimitive?.content

                if (thumbPath.isNullOrBlank() && thumbFileId != null && thumbFileId != 0) {
                    tracker.stickerThumbnails[thumbFileId] = stickerFileId
                    tdlibClient.send("""{"@type": "downloadFile", "file_id": $thumbFileId, "priority": 16, "offset": 0, "limit": 0, "synchronous": false}""")
                }

                list.add(
                    TelegramSticker(
                        fileId = stickerFileId,
                        remoteFileId = remoteFileId, // Сохраняем облачный ID!
                        emoji = emoji,
                        thumbnailPath = if (!thumbPath.isNullOrBlank()) thumbPath else null
                    )
                )
            }

            recentStickers.value = list
            return true
        }

        return false
    }
}