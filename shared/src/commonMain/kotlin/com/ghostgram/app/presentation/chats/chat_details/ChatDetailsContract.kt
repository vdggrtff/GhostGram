package com.ghostgram.app.presentation.chats.chat_details

import entity.Message

data class ChatDetailsState(
    val isLoading: Boolean = false,
    val chatTitle: String = "Чат",
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val errorMessage: String? = null
)

// Действия на экране чата
sealed interface ChatDetailsIntent {
    data class OnInputChanged(val text: String) : ChatDetailsIntent
    data object OnSendMessage : ChatDetailsIntent
}