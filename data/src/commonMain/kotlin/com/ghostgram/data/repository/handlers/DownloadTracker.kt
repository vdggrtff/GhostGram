package com.ghostgram.data.repository.handlers

// Объект, который помнит, какой fileId какому сообщению/чату принадлежит
class DownloadTracker {
    val chatAvatars = mutableMapOf<Int, Long>()   // fileId -> chatId
    val messagePhotos = mutableMapOf<Int, Long>() // Для картинок и превьюшек (photoPath)
    val messageFiles = mutableMapOf<Int, Long>()
    val stickerThumbnails = mutableMapOf<Int, Int>()
    var myAvatarFileId: Int? = null               // fileId твоей личной аватарки
}