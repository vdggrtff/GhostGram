package com.ghostgram.app.presentation.chats.chat_details

import AccountSession
import SessionManager
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.LoadMoreMessages
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnCancelEdit
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnCancelMediaSend
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnCancelReply
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnCatchUpClick
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnConfirmMediaSend
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnDeleteMessage
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnDismissCatchUpDialog
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnEditMessageClick
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnGenerateRepliesClick
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnInputChanged
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnMediaSelected
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnPendingCaptionChanged
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnSendMessage
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnSendSticker
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnSmartReplyClick
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnStartRecording
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnStopRecording
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnSwipeToReply
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnToggleCryptoMode
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnToggleGhostMode
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnToggleSendAsDocument
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnToggleStickers
import entity.Message
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import repository.ChatRepository
import usecase.GenerateCatchUpSummaryUseCase
import usecase.GenerateSmartRepliesUseCase

class ChatDetailsViewModel(
    savedStateHandle: SavedStateHandle, // Koin сам отдаст его сюда!
    private val sessionManager: SessionManager,
    private val generateSmartRepliesUseCase: GenerateSmartRepliesUseCase,
    private val generateCatchUpSummaryUseCase: GenerateCatchUpSummaryUseCase,
) : ViewModel() {
    //340
    val chatId: Long = savedStateHandle.get<Any>("chatId")?.toString()?.toLongOrNull() ?: 0L

    private val _state = MutableStateFlow(ChatDetailsState())
    val state: StateFlow<ChatDetailsState> = _state.asStateFlow()

    private var sessionJob: Job? = null
    private var isLoadingMore = false
    private var lastSummarizedMessageId: Long? = null
    private var cachedSummaryText: String? = null

    init {
        viewModelScope.launch {
            sessionManager.currentSession.collect { session ->
                if (session != null) {
                    subscribeToSession(session)
                }
            }
        }
    }

    fun onIntent(intent: ChatDetailsIntent) {
        val repo = sessionManager.currentSession.value?.chatRepository ?: return

        when (intent) {
            // Текст и сообщения
            is OnInputChanged -> _state.update { it.copy(inputText = intent.text) }
            is OnSendMessage -> sendMessage(repo)
            is OnEditMessageClick -> startEditing(intent.message)
            is OnCancelEdit -> _state.update { it.copy(editingMessage = null, inputText = "") }
            is OnSwipeToReply -> _state.update { it.copy(replyingToMessage = intent.message) }
            is OnCancelReply -> _state.update { it.copy(replyingToMessage = null) }
            is OnDeleteMessage -> viewModelScope.launch {
                repo.deleteMessage(
                    chatId,
                    intent.messageId,
                    intent.revoke
                )
            }

            is OnToggleCryptoMode -> toggleCrypto(repo)
            is OnToggleGhostMode -> repo.toggleGhostMode()

            // Медиа и Голосовые
            is OnMediaSelected -> _state.update {
                it.copy(
                    pendingMedia = intent.media,
                    pendingCaption = ""
                )
            }

            is OnPendingCaptionChanged -> _state.update { it.copy(pendingCaption = intent.text) }
            is OnToggleSendAsDocument -> _state.update { it.copy(sendAsDocument = intent.isChecked) }
            is OnCancelMediaSend -> _state.update { it.copy(pendingMedia = emptyList()) }
            is OnConfirmMediaSend -> sendMedia(repo)
            is OnToggleStickers -> toggleStickers(repo)
            is OnSendSticker -> sendSticker(repo, intent.remoteFileId)
            is OnStartRecording -> { /* UI-диктофон */
            }

            is OnStopRecording -> sendVoice(repo, intent)

            // ИИ и Пагинация
            is OnCatchUpClick -> generateCatchUp()
            is OnDismissCatchUpDialog -> _state.update { it.copy(catchUpSummary = null) }
            is OnGenerateRepliesClick -> generateReplies()
            is OnSmartReplyClick -> _state.update {
                it.copy(
                    inputText = intent.reply,
                    smartReplies = emptyList()
                )
            }

            is LoadMoreMessages -> loadMore(repo, intent.fromMessageId)
        }
    }

    // =========================================================================
    // 💥 СТРУКТУРИРОВАННЫЕ ПОДПИСКИ (Всё слушается в едином потоке)
    // =========================================================================
    private fun subscribeToSession(session: AccountSession) {
        sessionJob?.cancel()
        sessionJob = viewModelScope.launch {
            val repo = session.chatRepository
            repo.openChat(chatId)

            launch {
                repo.observeChat(chatId).collect { chat ->
                    if (chat != null) _state.update {
                        it.copy(
                            chatTitle = chat.title,
                            avatarPath = chat.avatarPath,
                            unreadCount = chat.unreadCount,
                            isGroup = chat.isGroup
                        )
                    }
                }
            }
            launch {
                repo.observeMessages(chatId).collect { messages ->
                    _state.update { it.copy(messages = messages) }
                }
            }
            launch {
                session.chatRepository.observeMyProfile().collect { profile ->
                    _state.update { it.copy(myAvatarPath = profile.avatarPath) }
                }
            }
            launch {
                repo.observeGhostMode().collect { isGhost ->
                    _state.update { it.copy(isGhostMode = isGhost) }
                }
            }
            launch {
                repo.observeRecentStickers().collect { stickers ->
                    _state.update { it.copy(recentStickers = stickers) }
                }
            }
        }
    }

    // =========================================================================
    // 💥 ДЕЛЕГИРОВАНИЕ ДЕЙСТВИЙ (Каждый метод отвечает за одну фичу)
    // =========================================================================
    private fun sendMessage(repo: ChatRepository) {
        val text = _state.value.inputText.trim()
        val useCrypto = _state.value.isCryptoMode
        val replyToId = _state.value.replyingToMessage?.id ?: 0L
        val editMsg = _state.value.editingMessage
        if (text.isBlank()) return

        viewModelScope.launch {
            if (editMsg != null) {
                repo.editMessageText(chatId, editMsg.id, text, useCrypto)
            } else {
                repo.sendMessage(chatId, text, useCrypto, replyToId)
            }
            _state.update {
                it.copy(
                    inputText = "",
                    replyingToMessage = null,
                    editingMessage = null
                )
            }
        }
    }

    private fun sendMedia(repo: ChatRepository) {
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
            if (mediaItems.size > 1 && !asDocument) {
                val payload = mediaItems.map { it.bytes to it.extension }
                repo.sendMediaAlbum(chatId, payload, caption, useCrypto, replyToId)
            } else {
                mediaItems.forEach { item ->
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
    }

    private fun startEditing(msg: Message) {
        _state.update {
            it.copy(
                editingMessage = msg,
                inputText = msg.text,
                isCryptoMode = msg.fileExtraInfo == "ENCRYPTED",
                replyingToMessage = null
            )
        }
    }

    private fun toggleCrypto(repo: ChatRepository) {
        val isCurrentlyCrypto = _state.value.isCryptoMode
        if (!isCurrentlyCrypto) {
            viewModelScope.launch { repo.requestKeyExchange(chatId) }
        }
        _state.update { it.copy(isCryptoMode = !isCurrentlyCrypto) }
    }

    private fun toggleStickers(repo: ChatRepository) {
        val isOpen = !_state.value.isStickersOpen
        _state.update { it.copy(isStickersOpen = isOpen) }
        if (isOpen) repo.loadRecentStickers()
    }

    private fun sendSticker(repo: ChatRepository, fileId: Int) {
        val replyToId = _state.value.replyingToMessage?.id ?: 0L
        viewModelScope.launch {
            repo.sendSticker(chatId, fileId, replyToId)
            _state.update { it.copy(isStickersOpen = false, replyingToMessage = null) }
        }
    }

    private fun sendVoice(repo: ChatRepository, intent: OnStopRecording) {
        if (!intent.send) return
        val replyToId = _state.value.replyingToMessage?.id ?: 0L
        viewModelScope.launch {
            repo.sendVoiceNote(chatId, intent.filePath, replyToId)
            _state.update { it.copy(replyingToMessage = null) }
        }
    }

    private fun loadMore(repo: ChatRepository, fromMessageId: Long) {
        if (isLoadingMore) return
        isLoadingMore = true
        viewModelScope.launch {
            repo.loadMoreMessages(chatId, fromMessageId)
            delay(500)
            isLoadingMore = false
        }
    }

    private fun generateCatchUp() {
        val currentLastId = _state.value.messages.lastOrNull()?.id
        if (currentLastId != null && currentLastId == lastSummarizedMessageId && cachedSummaryText != null) {
            _state.update { it.copy(catchUpSummary = cachedSummaryText) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isCatchUpLoading = true) }
            generateCatchUpSummaryUseCase(chatId)
                .onSuccess { summary ->
                    lastSummarizedMessageId = currentLastId
                    cachedSummaryText = summary
                    _state.update { it.copy(catchUpSummary = summary, isCatchUpLoading = false) }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isCatchUpLoading = false,
                            errorMessage = error.message
                        )
                    }
                }
        }
    }

    private fun generateReplies() {
        viewModelScope.launch {
            _state.update { it.copy(isRepliesLoading = true) }
            generateSmartRepliesUseCase(chatId)
                .onSuccess { replies ->
                    _state.update {
                        it.copy(
                            smartReplies = replies,
                            isRepliesLoading = false
                        )
                    }
                }
                .onFailure { _state.update { it.copy(isRepliesLoading = false) } }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sessionManager.currentSession.value?.chatRepository?.closeChat(chatId)
    }
}