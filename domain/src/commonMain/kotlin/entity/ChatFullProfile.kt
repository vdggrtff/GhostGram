package entity

data class ChatFullProfile(
    val id: Long,
    val title: String,
    val avatarPath: String? = null,
    val bio: String = "",          // Описание канала или «О себе» у юзера
    val username: String = "",     // @username
    val phoneNumber: String = "",  // Номер телефона (если открыт)
    val isGroup: Boolean = false,
    val isChannel: Boolean = false,
    val memberCount: Int = 0       // Количество участников для групп
)