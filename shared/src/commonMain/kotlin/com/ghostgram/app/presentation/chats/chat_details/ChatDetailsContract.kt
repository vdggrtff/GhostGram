package com.ghostgram.app.presentation.chats.chat_details

import entity.Message

data class ChatDetailsState(
    val isLoading: Boolean = false,
    val chatTitle: String = "Чат",
    val myAvatarPath: String? = null,
    val messages: List<Message> = emptyList(),
    val avatarPath: String? = null,
    val inputText: String = "",
    val errorMessage: String? = null,
    val smartReplies: List<String> = emptyList(), // Готовые ответы
    val isRepliesLoading: Boolean = false,        // Грузятся ли они сейчас
    val unreadCount: Int = 0,               // 💥 Число непрочитанных
    val catchUpSummary: String? = null,     // 💥 Текст выжимки от ИИ
    val isCatchUpLoading: Boolean = false,
    val isGhostMode: Boolean = true,
    val isCryptoMode: Boolean = false,
)

// Действия на экране чата
sealed interface ChatDetailsIntent {
    data class OnInputChanged(val text: String) : ChatDetailsIntent
    data object OnSendMessage : ChatDetailsIntent

    data object OnGenerateRepliesClick : ChatDetailsIntent
    data class OnSmartReplyClick(val reply: String) : ChatDetailsIntent

    data object OnCatchUpClick : ChatDetailsIntent
    data object OnDismissCatchUpDialog : ChatDetailsIntent
    data object OnToggleGhostMode : ChatDetailsIntent
    data object OnToggleCryptoMode : ChatDetailsIntent

    data class LoadMoreMessages(val fromMessageId: Long) : ChatDetailsIntent
}