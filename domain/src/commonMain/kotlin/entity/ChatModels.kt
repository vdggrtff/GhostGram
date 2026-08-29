package entity

data class Message(
    val id: Long,
    val chatId: Long,
    val senderName: String,
    val text: String,
    val isDeletedLocally: Boolean = false // Киллер-фича Anti-Revoke!
)

// Наша независимая доменная модель чата
data class Chat(
    val id: Long,
    val title: String,
    val unreadCount: Int,
    val lastMessage: Message?
)