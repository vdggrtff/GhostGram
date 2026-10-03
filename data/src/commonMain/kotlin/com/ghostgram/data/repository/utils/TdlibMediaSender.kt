package com.ghostgram.data.repository.utils

import com.ghostgram.core.crypto.CryptoLayer
import com.ghostgram.core.tdlib.TelegramFlowClient
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okio.FileSystem
import okio.SYSTEM
import kotlin.time.Clock.System

class TdlibMediaSender(
    private val tdlibClient: TelegramFlowClient,
    private val cryptoLayer: CryptoLayer
) {
    suspend fun sendText(chatId: Long, text: String, useCrypto: Boolean, replyToId: Long) {
        val finalText = if (useCrypto) {
            if (!cryptoLayer.isChatSecure(chatId)) text else cryptoLayer.encryptAndHide(chatId, text)
        } else text

        val request = buildJsonObject {
            put("@type", "sendMessage")
            put("chat_id", chatId)
            if (replyToId != 0L) {
                put("reply_to", buildJsonObject {
                    put("@type", "inputMessageReplyToMessage")
                    put("message_id", replyToId)
                })
            }
            put("input_message_content", buildJsonObject {
                put("@type", "inputMessageText")
                put("text", buildJsonObject {
                    put("@type", "formattedText")
                    put("text", finalText)
                })
                put("link_preview_options", buildJsonObject {
                    put("@type", "linkPreviewOptions")
                    put("is_disabled", true)
                })
            })
        }
        tdlibClient.send(request.toString())
    }

    suspend fun sendMedia(chatId: Long, bytes: ByteArray, extension: String, caption: String, useCrypto: Boolean, asDocument: Boolean, replyToId: Long) {
        if (bytes.isEmpty()) return
        val finalCaption = if (useCrypto) cryptoLayer.encryptAndHide(chatId, caption) else caption

        val fs = FileSystem.SYSTEM
        val tempDir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "ghostgram_temp"
        if (!fs.exists(tempDir)) fs.createDirectories(tempDir)

        val ext = extension.lowercase().ifBlank { if (asDocument) "png" else "jpg" }
        val tempFile = tempDir / "ghost_${System.now().toEpochMilliseconds()}.$ext"
        fs.write(tempFile) { write(bytes) }
        val absolutePath = tempFile.toString().replace("\\", "/")
        val isVideo = ext in listOf("mp4", "mov", "mkv", "avi")

        val request = buildJsonObject {
            put("@type", "sendMessage")
            put("chat_id", chatId)
            if (replyToId != 0L) {
                put("reply_to", buildJsonObject {
                    put("@type", "inputMessageReplyToMessage")
                    put("message_id", replyToId)
                })
            }
            put("input_message_content", buildJsonObject {
                when {
                    asDocument -> {
                        put("@type", "inputMessageDocument")
                        put("document", buildJsonObject {
                            put("@type", "inputDocument")
                            put("document", buildJsonObject { put("@type", "inputFileLocal"); put("path", absolutePath) })
                        })
                    }
                    isVideo -> {
                        put("@type", "inputMessageVideo")
                        put("video", buildJsonObject {
                            put("@type", "inputVideo")
                            put("video", buildJsonObject { put("@type", "inputFileLocal"); put("path", absolutePath) })
                        })
                    }
                    else -> {
                        put("@type", "inputMessagePhoto")
                        put("photo", buildJsonObject {
                            put("@type", "inputPhoto")
                            put("photo", buildJsonObject { put("@type", "inputFileLocal"); put("path", absolutePath) })
                        })
                    }
                }
                if (finalCaption.isNotBlank()) {
                    put("caption", buildJsonObject {
                        put("@type", "formattedText")
                        put("text", finalCaption.trim())
                    })
                }
            })
        }
        tdlibClient.send(request.toString())
    }

    suspend fun sendMediaAlbum(chatId: Long, media: List<Pair<ByteArray, String>>, caption: String, useCrypto: Boolean, replyToId: Long) {
        if (media.isEmpty()) return
        val finalCaption = if (useCrypto) cryptoLayer.encryptAndHide(chatId, caption) else caption

        val fs = FileSystem.SYSTEM
        val tempDir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "ghostgram_temp"
        if (!fs.exists(tempDir)) fs.createDirectories(tempDir)

        media.chunked(10).forEach { chunk ->
            val inputContents = chunk.mapIndexed { index, (bytes, extension) ->
                val ext = extension.lowercase().ifBlank { "jpg" }
                val tempFile = tempDir / "ghost_album_${System.now().toEpochMilliseconds()}_$index.$ext"
                fs.write(tempFile) { write(bytes) }
                val absolutePath = tempFile.toString().replace("\\", "/")
                val isVideo = ext in listOf("mp4", "mov", "mkv", "avi")

                buildJsonObject {
                    if (isVideo) {
                        put("@type", "inputMessageVideo")
                        put("video", buildJsonObject {
                            put("@type", "inputVideo")
                            put("video", buildJsonObject { put("@type", "inputFileLocal"); put("path", absolutePath) })
                        })
                    } else {
                        put("@type", "inputMessagePhoto")
                        put("photo", buildJsonObject {
                            put("@type", "inputPhoto")
                            put("photo", buildJsonObject { put("@type", "inputFileLocal"); put("path", absolutePath) })
                        })
                    }
                    if (index == 0 && finalCaption.isNotBlank()) {
                        put("caption", buildJsonObject {
                            put("@type", "formattedText")
                            put("text", finalCaption.trim())
                        })
                    }
                }
            }

            val request = buildJsonObject {
                put("@type", "sendMessageAlbum")
                put("chat_id", chatId)
                if (replyToId != 0L) {
                    put("reply_to", buildJsonObject {
                        put("@type", "inputMessageReplyToMessage")
                        put("message_id", replyToId)
                    })
                }
                put("input_message_contents", buildJsonArray {
                    inputContents.forEach { add(it) }
                })
            }
            tdlibClient.send(request.toString())
        }
    }

    suspend fun sendSticker(chatId: Long, stickerFileId: Int, replyToId: Long) {
        val request = buildJsonObject {
            put("@type", "sendMessage")
            put("chat_id", chatId)
            if (replyToId != 0L) {
                put("reply_to", buildJsonObject {
                    put("@type", "inputMessageReplyToMessage")
                    put("message_id", replyToId)
                })
            }
            put("input_message_content", buildJsonObject {
                put("@type", "inputMessageSticker")
                put("sticker", buildJsonObject {
                    put("@type", "inputSticker")
                    put("sticker", buildJsonObject {
                        put("@type", "inputFileId")
                        put("id", stickerFileId)
                    })
                })
            })
        }
        tdlibClient.send(request.toString())
    }

    suspend fun sendVoiceNote(chatId: Long, filePath: String, replyToId: Long) {
        val request = buildJsonObject {
            put("@type", "sendMessage")
            put("chat_id", chatId)
            if (replyToId != 0L) {
                put("reply_to", buildJsonObject {
                    put("@type", "inputMessageReplyToMessage")
                    put("message_id", replyToId)
                })
            }
            put("input_message_content", buildJsonObject {
                put("@type", "inputMessageVoiceNote")
                put("voice_note", buildJsonObject {
                    put("@type", "inputVoiceNote")
                    put("voice_note", buildJsonObject {
                        put("@type", "inputFileLocal")
                        put("path", filePath)
                    })
                })
            })
        }
        tdlibClient.send(request.toString())
    }
}