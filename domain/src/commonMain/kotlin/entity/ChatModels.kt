package entity

data class Message(
    val id: Long,
    val chatId: Long,
    val senderName: String,
    val text: String,
    val isOutgoing: Boolean = false,
    val isDeletedLocally: Boolean = false,
    val photoPath: String? = null // 💥 Локальный путь к скачанной фотографии
)

data class Chat(
    val id: Long,
    val title: String,
    val unreadCount: Int,
    val lastMessage: Message?,
    val avatarPath: String? = null // 💥 Локальный путь к аватарке
)