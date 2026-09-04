package com.ghostgram.app.presentation.chats.chat_details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import repository.ChatRepository

class ChatDetailsViewModel(
    savedStateHandle: SavedStateHandle, // 💥 Koin сам отдаст его сюда!
    private val chatRepository: ChatRepository
) : ViewModel() {

    // Достаем аргумент из навигации (ключ "chatId" должен совпадать с тем, что в navArgument)
    val chatId: Long = savedStateHandle.get<Long>("chatId") ?: savedStateHandle.get<String>("chatId")?.toLongOrNull() ?: 0L

    private val _state = MutableStateFlow(ChatDetailsState())
    val state: StateFlow<ChatDetailsState> = _state.asStateFlow()

    init {
        loadMyAvatar()
        loadChatInfo()
        loadMessages()
    }

    fun onIntent(intent: ChatDetailsIntent) {
        when (intent) {
            is ChatDetailsIntent.OnInputChanged -> _state.update { it.copy(inputText = intent.text) }
            is ChatDetailsIntent.OnSendMessage -> sendMessage()
                // UI сам обработает клик назад, ViewModel тут просто для логов/аналитики
        }
    }

    private fun loadChatInfo() {
        viewModelScope.launch {
            chatRepository.observeChat(chatId).collect { chat ->
                if (chat != null) {
                    _state.update {
                        it.copy(
                            chatTitle = chat.title,
                            avatarPath = chat.avatarPath
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

    /*private fun loadChatDetails() {
        if (chatId == 0L) {
            _state.update { it.copy(errorMessage = "Ошибка: неверный ID чата") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            // В будущем тут будет observeMessages(chatId)
            val history = chatRepository.getChatHistory(chatId = chatId, limit = 50)

            _state.update {
                it.copy(
                    isLoading = false,
                    chatTitle = "Чат #$chatId", // Временно хардкодим название
                    chatHistory = history
                )
            }
        }
    }*/
}