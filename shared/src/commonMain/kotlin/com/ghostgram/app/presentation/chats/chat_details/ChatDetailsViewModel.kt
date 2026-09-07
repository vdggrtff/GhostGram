package com.ghostgram.app.presentation.chats.chat_details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnCatchUpClick
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnDismissCatchUpDialog
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnGenerateRepliesClick
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnInputChanged
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnSendMessage
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnSmartReplyClick
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsIntent.OnToggleGhostMode
import com.ghostgram.core.crypto.CryptoLayer
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
    private val chatRepository: ChatRepository,
    private val generateSmartRepliesUseCase: GenerateSmartRepliesUseCase,
    private val generateCatchUpSummaryUseCase: GenerateCatchUpSummaryUseCase
) : ViewModel() {

    // Достаем аргумент из навигации (ключ "chatId" должен совпадать с тем, что в navArgument)
    val chatId: Long = savedStateHandle.get<Long>("chatId") ?: savedStateHandle.get<String>("chatId")?.toLongOrNull() ?: 0L

    private var lastSummarizedMessageId: Long? = null
    private var cachedSummaryText: String? = null

    private val _state = MutableStateFlow(ChatDetailsState())
    val state: StateFlow<ChatDetailsState> = _state.asStateFlow()

    init {
        loadMyAvatar()
        loadChatInfo()
        loadMessages()
        observeGhostMode()

        val crypto = CryptoLayer()
        val aliceKeys = crypto.createKeyPair()
        val bobKeys = crypto.createKeyPair()

// Алиса и Боб вычисляют общий секрет (Магия математики!)
        val aliceShared = crypto.getSharedSecret(aliceKeys.first, bobKeys.second)
        val bobShared = crypto.getSharedSecret(bobKeys.first, aliceKeys.second)

        println("Секреты совпали? ${aliceShared.contentEquals(bobShared)}")

        val originalText = "Товарищ майор это не прочитает!"
        val hiddenMessage = crypto.encryptAndHide(originalText, aliceShared)

        println("ШИФРОВКА: $hiddenMessage")

        val decrypted = crypto.revealAndDecrypt(hiddenMessage, bobShared)
        println("РАСШИФРОВКА: $decrypted")
    }

    fun onIntent(intent: ChatDetailsIntent) {
        when (intent) {
            is OnInputChanged -> _state.update { it.copy(inputText = intent.text) }
            is OnSendMessage -> sendMessage()
            is OnSmartReplyClick -> {
                // При клике на чип — текст вставляется в инпут!
                _state.update { it.copy(inputText = intent.reply, smartReplies = emptyList()) }
            }
            is OnGenerateRepliesClick -> generateReplies()
            is OnCatchUpClick -> generateCatchUp()
            is OnDismissCatchUpDialog -> _state.update { it.copy(catchUpSummary = null) }
            is OnToggleGhostMode -> chatRepository.toggleGhostMode()
        }
    }

    private fun loadChatInfo() {
        viewModelScope.launch {
            chatRepository.observeChat(chatId).collect { chat ->
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
    private fun loadMyAvatar() {
        viewModelScope.launch {
            chatRepository.observeMyAvatar().collect { path ->
                _state.update { it.copy(myAvatarPath = path) }
            }
        }
    }

    private fun loadMessages() {
        viewModelScope.launch {
            chatRepository.observeMessages(chatId).collect { messageList ->
                _state.update { it.copy(messages = messageList) }
            }
        }
    }

    private fun sendMessage() {
        val text = _state.value.inputText.trim()
        if (text.isBlank()) return

        viewModelScope.launch {
            chatRepository.sendMessage(chatId, text)
            _state.update { it.copy(inputText = "") } // Очищаем поле ввода
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

    private fun observeGhostMode() {
        viewModelScope.launch {
            chatRepository.observeGhostMode().collect { isGhost ->
                _state.update { it.copy(isGhostMode = isGhost) }
            }
        }
    }
}