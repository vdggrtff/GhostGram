package entity

import kotlinx.serialization.Serializable

@Serializable
data class MessageReactionItem(
    val emoji: String,
    val count: Int,
    val isChosen: Boolean,
    val customEmojiId: Long = 0L,
    val customEmojiPath: String? = null
)