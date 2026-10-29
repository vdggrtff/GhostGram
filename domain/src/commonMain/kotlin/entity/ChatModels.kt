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
    val photoPath: String? = null,
    val mediaType: MessageMediaType = MessageMediaType.TEXT,
    val fileName: String? = null,
    val fileExtraInfo: String? = null,   // Размер ("12.4 MB"), длительность ("0:45") или эмодзи стикера
    val isRead: Boolean = false,
    val date: Int = 0,
    val mediaAlbumId: Long = 0L,
    val isSending: Boolean = false,
    val replyToMessageId: Long = 0L,
    val isEdited: Boolean = false,
    val senderId: Long = 0L,
    val senderAvatarPath: String? = null,
    val waveform: String? = null,
    val reactions: List<MessageReactionItem> = emptyList()
)

data class Chat(
    val id: Long,
    val title: String,
    val unreadCount: Int,
    val lastMessage: Message?,
    val avatarPath: String? = null,
    val isJoined: Boolean = false,
    val order: Long = 0L,
    val isGroup: Boolean = false,
    val pinnedMessageId: Long = 0L,
    val isChannel: Boolean = false,
    val memberCount: Int = 0,
    val isMuted: Boolean = false
    //val isOnline: Boolean = false,
)

data class UserOnlineStatus(
    val isOnline: Boolean = false,
    val wasOnline: Int = 0,
    val statusType: String = ""
)