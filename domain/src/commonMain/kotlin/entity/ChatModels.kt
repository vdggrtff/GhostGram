package entity

data class Message(
    val id: Long,
    val chatId: Long,
    val senderName: String,
    val text: String,
    val isOutgoing: Boolean = false, // 💥 True, если сообщение отправил ТЫ
    val isDeletedLocally: Boolean = false
)

data class Chat(
    val id: Long,
    val title: String,
    val unreadCount: Int,
    val lastMessage: Message?
)