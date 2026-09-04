package com.ghostgram.app.presentation.chats

import entity.Chat

data class ChatListState(
    val isLoading: Boolean = false,
    val chats: List<Chat> = emptyList(),
    val isSummarizing: Boolean = false, // Крутится ли лоадер для Gemini
    val aiSummaryText: String? = null,   // Текст ответа от Gemini
    val errorMessage: String? = null
)

// 2. Все возможные действия пользователя на этом экране
sealed interface ChatListIntent {
    data object LoadChats : ChatListIntent
    data class OnSummarizeChatClick(val chatId: Long) : ChatListIntent
    data object OnDismissSummaryDialog : ChatListIntent
}