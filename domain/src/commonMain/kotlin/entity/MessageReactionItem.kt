package entity

import kotlinx.serialization.Serializable

@Serializable
data class MessageReactionItem(
    val emoji: String,
    val count: Int,
    val isChosen: Boolean // Поставил ли эту реакцию текущий пользователь
)