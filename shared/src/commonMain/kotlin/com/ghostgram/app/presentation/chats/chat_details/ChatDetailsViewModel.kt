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
    val chatId: Long = savedStateHandle.get<Long>("chatId") ?: 0L

    private val _state = MutableStateFlow(ChatDetailsState())
    val state: StateFlow<ChatDetailsState> = _state.asStateFlow()

    init {
        onIntent(ChatDetailsIntent.LoadChatInfo)
    }

    fun onIntent(intent: ChatDetailsIntent) {
        when (intent) {
            is ChatDetailsIntent.LoadChatInfo -> loadChatDetails()
            is ChatDetailsIntent.OnBackClicked -> {
                // UI сам обработает клик назад, ViewModel тут просто для логов/аналитики
            }
        }
    }

    private fun loadChatDetails() {
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
    }
}