package com.ghostgram.app.presentation.chats.chat_details

data class ChatDetailsState(
    val isLoading: Boolean = false,
    val chatTitle: String = "Загрузка...",
    val chatHistory: String = "", // Пока храним как строку для ИИ, потом сделаем List<Message>
    val errorMessage: String? = null
)

// Действия на экране чата
sealed interface ChatDetailsIntent {
    data object LoadChatInfo : ChatDetailsIntent
    data object OnBackClicked : ChatDetailsIntent
}