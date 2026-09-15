package com.ghostgram.app.presentation.chats

import entity.Chat
import entity.PublicChat

data class ChatListState(
    val isLoading: Boolean = false,
    val chats: List<Chat> = emptyList(),
    val isSummarizing: Boolean = false, // Крутится ли лоадер для Gemini
    val aiSummaryText: String? = null,   // Текст ответа от Gemini
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val globalSearchResults: List<PublicChat> = emptyList(),
    val isSearching: Boolean = false,
    val messageSearchResults: List<Chat> = emptyList()
)

// 2. Все возможные действия пользователя на этом экране
sealed interface ChatListIntent {
    data object LoadChats : ChatListIntent
    data class OnSummarizeChatClick(val chatId: Long) : ChatListIntent
    data object OnDismissSummaryDialog : ChatListIntent

    data class OnSearchQueryChanged(val query: String) : ChatListIntent
}