package entity

enum class MessageMediaType {
    TEXT, PHOTO, DOCUMENT, VOICE, VIDEO, VIDEO_NOTE, STICKER
}

data class Message(
    val id: Long,
    val chatId: Long,
    val senderName: String,
    val text: String,
    val isOutgoing: Boolean = false,
    val isDeletedLocally: Boolean = false,
    val photoPath: String? = null, // 💥 Локальный путь к скачанной фотографии
    val mediaType: MessageMediaType = MessageMediaType.TEXT,
    val fileName: String? = null,       // Например: "document.pdf"
    val fileExtraInfo: String? = null   // Размер ("12.4 MB"), длительность ("0:45") или эмодзи стикера
)

data class Chat(
    val id: Long,
    val title: String,
    val unreadCount: Int,
    val lastMessage: Message?,
    val avatarPath: String? = null // 💥 Локальный путь к аватарке
)