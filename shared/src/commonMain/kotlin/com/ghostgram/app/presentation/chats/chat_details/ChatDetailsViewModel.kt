package com.ghostgram.app.presentation.chats.chat_details

import AccountSession
import SessionManager
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.LoadMoreMessages
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnCancelMediaSend
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnCancelReply
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnCatchUpClick
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnConfirmMediaSend
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnDeleteMessage
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnDismissCatchUpDialog
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnGenerateRepliesClick
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnInputChanged
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnMediaSelected
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnPendingCaptionChanged
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnSendMessage
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnSmartReplyClick
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnSwipeToReply
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnToggleCryptoMode
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnToggleGhostMode
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnToggleSendAsDocument
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import repository.ChatRepository
import usecase.GenerateCatchUpSummaryUseCase
import usecase.GenerateSmartRepliesUseCase

class ChatDetailsViewModel(
    savedStateHandle: SavedStateHandle, // 💥 Koin сам отдаст его сюда!
    private val sessionManager: SessionManager,
    private val generateSmartRepliesUseCase: GenerateSmartRepliesUseCase,
    private val generateCatchUpSummaryUseCase: GenerateCatchUpSummaryUseCase,
) : ViewModel() {

    // Достаем аргумент из навигации (ключ "chatId" должен совпадать с тем, что в navArgument)
    val chatId: Long =
        savedStateHandle.get<Long>("chatId") ?: savedStateHandle.get<String>("chatId")
            ?.toLongOrNull() ?: 0L

    private val _state = MutableStateFlow(ChatDetailsState())
    val state: StateFlow<ChatDetailsState> = _state.asStateFlow()

    private var messagesJob: Job? = null
    private var chatInfoJob: Job? = null
    private var avatarJob: Job? = null
    private var ghostModeJob: Job? = null

    private var lastSummarizedMessageId: Long? = null
    private var cachedSummaryText: String? = null

    private var isLoadingMore = false

    init {
        println("🔍 ChatDetailsViewModel запущен для chatId: $chatId")
        viewModelScope.launch {
            sessionManager.currentSession.collect { session ->
                if (session != null) {
                    val repo = session.chatRepository
                    loadChatInfo(repo)
                    loadMessages(repo)
                    loadMyAvatar(session)
                    observeGhostMode(repo)
                }
            }
        }
    }

    fun onIntent(intent: ChatDetailsIntent) {
        val repo = sessionManager.currentSession.value?.chatRepository ?: return

        when (intent) {
            is OnInputChanged -> _state.update { it.copy(inputText = intent.text) }
            is OnToggleCryptoMode -> {
                viewModelScope.launch {
                    val isCurrentlyCrypto = _state.value.isCryptoMode

                    if (!isCurrentlyCrypto) {
                        // 💥 Если режим БЫЛ ВЫКЛЮЧЕН, то мы его включаем и шлем публичный ключ собеседнику!
                        viewModelScope.launch {
                            repo.requestKeyExchange(chatId)
                        }
                    }

                    _state.update { it.copy(isCryptoMode = !isCurrentlyCrypto) }
                }
            }

            is OnToggleGhostMode -> repo.toggleGhostMode()
            /*is OnSendMessage -> {
                val text = _state.value.inputText.trim()
                val useCrypto = _state.value.isCryptoMode
                if (text.isBlank()) return

                viewModelScope.launch {
                    repo.sendMessage(chatId, text, useCrypto)
                    _state.update { it.copy(inputText = "") }
                }
            }*/
            is OnSendMessage -> {
                val text = _state.value.inputText.trim()
                val useCrypto = _state.value.isCryptoMode
                val replyToId = _state.value.replyingToMessage?.id ?: 0L // 💥 БЕРЕМ ID ОТВЕТА
                if (text.isBlank()) return

                viewModelScope.launch {
                    repo.sendMessage(chatId, text, useCrypto, replyToId) // 💥 ПЕРЕДАЕМ ID
                    _state.update { it.copy(inputText = "", replyingToMessage = null) } // Очищаем всё
                }
            }
            is OnGenerateRepliesClick -> generateReplies()
            is OnSmartReplyClick -> _state.update {
                it.copy(
                    inputText = intent.reply,
                    smartReplies = emptyList()
                )
            }

            is OnCatchUpClick -> generateCatchUp()
            is OnDismissCatchUpDialog -> _state.update { it.copy(catchUpSummary = null) }
            is LoadMoreMessages -> {
                if (isLoadingMore) return
                isLoadingMore = true
                viewModelScope.launch {
                    repo.loadMoreMessages(chatId, intent.fromMessageId)
                    kotlinx.coroutines.delay(500) // Даем базе время записать данные
                    isLoadingMore = false
                }
            }
            is OnMediaSelected -> {
                _state.update { it.copy(pendingMedia = intent.media, pendingCaption = "") }
            }
            /*is OnConfirmMediaSend -> {
                val mediaItems = _state.value.pendingMedia
                val caption = _state.value.pendingCaption.trim()
                val useCrypto = _state.value.isCryptoMode
                val asDocument = _state.value.sendAsDocument

                _state.update { it.copy(pendingMedia = emptyList(), pendingCaption = "", sendAsDocument = false) }

                viewModelScope.launch {
                    mediaItems.forEach { item ->
                        // 💥 Передаем байты и расширение!
                        repo.sendMedia(chatId, item.bytes, item.extension, caption, useCrypto, asDocument)
                    }
                }
            }*/
            is OnConfirmMediaSend -> {
                val mediaItems = _state.value.pendingMedia
                val caption = _state.value.pendingCaption.trim()
                val useCrypto = _state.value.isCryptoMode
                val asDocument = _state.value.sendAsDocument
                val replyToId = _state.value.replyingToMessage?.id ?: 0L

                _state.update {
                    it.copy(
                        pendingMedia = emptyList(),
                        pendingCaption = "",
                        sendAsDocument = false,
                        replyingToMessage = null
                    )
                }

                viewModelScope.launch {
                    mediaItems.forEach { item ->
                        // 💥 Передаем байты и расширение!
                        repo.sendMedia(
                            chatId,
                            item.bytes,
                            item.extension,
                            caption,
                            useCrypto,
                            asDocument,
                            replyToId
                        )
                    }
                }
            }
            is OnPendingCaptionChanged -> {
                _state.update { it.copy(pendingCaption = intent.text) }
            }
            is OnToggleSendAsDocument -> {
                _state.update { it.copy(sendAsDocument = intent.isChecked) }
            }
            is OnCancelMediaSend -> {
                _state.update { it.copy(pendingMedia = emptyList()) }
            }
            is OnDeleteMessage -> {
                viewModelScope.launch {
                    repo.deleteMessage(chatId, intent.messageId, intent.revoke)
                }
            }
            is OnSwipeToReply -> _state.update { it.copy(replyingToMessage = intent.message) }
            is OnCancelReply -> _state.update { it.copy(replyingToMessage = null) }
        }
    }

    private fun loadChatInfo(repo: ChatRepository) {
        chatInfoJob?.cancel()
        chatInfoJob = viewModelScope.launch {
            repo.observeChat(chatId).collect { chat ->
                if (chat != null) {
                    _state.update {
                        it.copy(
                            chatTitle = chat.title,
                            avatarPath = chat.avatarPath,
                            unreadCount = chat.unreadCount
                        )
                    }
                }
            }
        }
    }

    private fun loadMyAvatar(session: AccountSession) {
        avatarJob?.cancel()
        avatarJob = viewModelScope.launch {
            session.chatRepository.observeMyProfile().collect { profile ->
                _state.update {
                    it.copy(
                        avatarPath = profile.avatarPath, // В ChatDetails тебе нужен только avatarPath и myAvatarPath
                    )
                }
            }
        }
    }

    private fun loadMessages(repo: ChatRepository) {
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            repo.observeMessages(chatId).collect { messageList ->
                _state.update { it.copy(messages = messageList) }
            }
        }
    }
    private fun generateCatchUp() {
        val currentLastMessageId = _state.value.messages.lastOrNull()?.id

        // 💥 ПРОВЕРКА КЭША: если сообщений не прибавилось — отдаем старую выжимку бесплатно!
        if (currentLastMessageId != null && currentLastMessageId == lastSummarizedMessageId && cachedSummaryText != null) {
            _state.update { it.copy(catchUpSummary = cachedSummaryText) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isCatchUpLoading = true) }
            val result = generateCatchUpSummaryUseCase(chatId)

            result.onSuccess { summary ->
                // Сохраняем в кэш ID последнего сообщения и текст
                lastSummarizedMessageId = currentLastMessageId
                cachedSummaryText = summary

                _state.update { it.copy(catchUpSummary = summary, isCatchUpLoading = false) }
            }.onFailure { error ->
                _state.update { it.copy(isCatchUpLoading = false, errorMessage = error.message) }
            }
        }
    }

    private fun generateReplies() {
        viewModelScope.launch {
            _state.update { it.copy(isRepliesLoading = true) }
            val result = generateSmartRepliesUseCase(chatId)

            result.onSuccess { replies ->
                _state.update { it.copy(smartReplies = replies, isRepliesLoading = false) }
            }.onFailure {
                _state.update { it.copy(isRepliesLoading = false) }
            }
        }
    }

    private fun observeGhostMode(repo: ChatRepository) {
        ghostModeJob?.cancel()
        ghostModeJob = viewModelScope.launch {
            repo.observeGhostMode().collect { isGhost ->
                _state.update { it.copy(isGhostMode = isGhost) }
            }
        }
    }
}