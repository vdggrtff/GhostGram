package com.ghostgram.app.presentation.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import repository.ChatRepository
import usecase.GenerateChatSummaryUseCase

class ChatListViewModel(
    private val chatRepository: ChatRepository,
    private val generateChatSummaryUseCase: GenerateChatSummaryUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatListState())
    val state: StateFlow<ChatListState> = _state.asStateFlow()

    init {
        onIntent(ChatListIntent.LoadChats)
    }

    fun onIntent(intent: ChatListIntent) {
        when (intent) {
            is ChatListIntent.LoadChats -> loadChats()
            is ChatListIntent.OnSummarizeChatClick -> summarizeChat(intent.chatId)
            is ChatListIntent.OnDismissSummaryDialog -> dismissSummary()
        }
    }

    private fun loadChats() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            chatRepository.observeChats().collect { chatList ->
                _state.update {
                    it.copy(isLoading = false, chats = chatList)
                }
            }
        }
    }

    private fun summarizeChat(chatId: Long) {
        viewModelScope.launch {
            // Включаем крутилку лоадера для AI
            _state.update { it.copy(isSummarizing = true, errorMessage = null) }

            generateChatSummaryUseCase(chatId).fold(
                onSuccess = { summary ->
                    _state.update {
                        it.copy(
                            isSummarizing = false,
                            aiSummaryText = summary
                        )
                    }
                },
                onFailure = {error ->
                    _state.update {
                        it.copy(
                            isSummarizing = false,
                            errorMessage = error.message ?: "Ошибка генерации выжимки"
                        )
                    }
                }
            )
        }
    }

    private fun dismissSummary() {
        _state.update { it.copy(aiSummaryText = null, errorMessage = null) }
    }
}