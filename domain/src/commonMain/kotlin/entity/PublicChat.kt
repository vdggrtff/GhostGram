package entity

data class PublicChat(
    val id: Long,
    val title: String,
    val username: String,
    val avatarPath: String? = null,
    val isChannel: Boolean = true
)