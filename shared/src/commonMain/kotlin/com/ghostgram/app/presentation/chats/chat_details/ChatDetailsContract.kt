package com.ghostgram.app.presentation.chats.chat_details

import entity.Message

data class MediaItem(val bytes: ByteArray, val extension: String)

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
    val unreadCount: Int = 0,               // Число непрочитанных
    val catchUpSummary: String? = null,     // Текст выжимки от ИИ
    val isCatchUpLoading: Boolean = false,
    val isGhostMode: Boolean = true,
    val isCryptoMode: Boolean = false,
    val pendingMedia: List<MediaItem> = emptyList(), // Выбранные фотки
    val pendingCaption: String = "",                 // Подпись в диалоге
    val sendAsDocument: Boolean = false,              // Галочка "Отправить как файл"
    val replyingToMessage: Message? = null,
    val editingMessage: Message? = null,
    val recentStickers: List<entity.TelegramSticker> = emptyList(), // Список стикеров
    val isStickersOpen: Boolean = false,
    val isGroup: Boolean = false,
    val isSearchOpen: Boolean = false,              // Открыта ли строка поиска в TopBar
    val inChatSearchQuery: String = "",             // Текст поиска
    val inChatSearchResults: List<Message> = emptyList(), // Список найденных сообщений
    val currentSearchIndex: Int = 0,                // На каком сообщении мы сейчас (0, 1, 2...)
    val isSearchingInChat: Boolean = false,
    val isAiSearchEnabled: Boolean = false,
    val isAiSearching: Boolean = false
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
    data class OnPendingCaptionChanged(val text: String) : ChatDetailsIntent
    data class OnToggleSendAsDocument(val isChecked: Boolean) : ChatDetailsIntent
    data object OnCancelMediaSend : ChatDetailsIntent
    data object OnConfirmMediaSend : ChatDetailsIntent

    data class OnMediaSelected(val media: List<MediaItem>) : ChatDetailsIntent

    data class OnDeleteMessage(val messageId: Long, val revoke: Boolean) : ChatDetailsIntent

    data class OnSwipeToReply(val message: Message) : ChatDetailsIntent // Свайпнули!
    data object OnCancelReply : ChatDetailsIntent // Передумали отвечать
    data class OnEditMessageClick(val message: Message) : ChatDetailsIntent // Клик в меню
    data object OnCancelEdit : ChatDetailsIntent
    data object OnToggleStickers : ChatDetailsIntent // Клик по смайлику
    data class OnSendSticker(val remoteFileId: Int) : ChatDetailsIntent
    data class OnStartRecording(val filePath: String) : ChatDetailsIntent
    data class OnStopRecording(val send: Boolean, val filePath: String) : ChatDetailsIntent

    data class OnToggleInChatSearch(val isOpen: Boolean) : ChatDetailsIntent // Открыть/закрыть
    data class OnInChatSearchQueryChanged(val query: String) : ChatDetailsIntent // Печатаем
    data object OnSearchNext : ChatDetailsIntent // Кнопка 🔽 (Свежее)
    data object OnSearchPrevious : ChatDetailsIntent
    data object OnToggleAiSearch : ChatDetailsIntent
    data object OnRunAiSearch : ChatDetailsIntent
}