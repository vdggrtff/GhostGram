package com.ghostgram.app.presentation.chats

import SessionManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import repository.ChatRepository
import usecase.GenerateChatSummaryUseCase

class ChatListViewModel(
    private val generateChatSummaryUseCase: GenerateChatSummaryUseCase,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(ChatListState())
    val state: StateFlow<ChatListState> = _state.asStateFlow()

    private var currentChatsJob: Job? = null

    init {
        viewModelScope.launch {
            sessionManager.currentSession.collect { session ->
                if (session != null) {
                    loadChatsForSession(session.chatRepository)
                } else {
                    _state.update { it.copy(chats = emptyList()) }
                }
            }
        }
    }

    fun onIntent(intent: ChatListIntent) {
        when (intent) {
            is ChatListIntent.LoadChats -> {
                val repo = sessionManager.currentSession.value?.chatRepository
                if (repo != null) loadChatsForSession(repo)
            }
            //is ChatListIntent.LoadChats -> loadChats()
            is ChatListIntent.OnSummarizeChatClick -> summarizeChat(intent.chatId)
            is ChatListIntent.OnDismissSummaryDialog -> dismissSummary()
        }
    }

    private fun loadChatsForSession(chatRepository: ChatRepository) {
        // Отменяем прослушивание старого аккаунта, если оно было
        currentChatsJob?.cancel()

        _state.update { it.copy(isLoading = true) }

        currentChatsJob = viewModelScope.launch {
            chatRepository.observeChats().collect { chatList ->
                _state.update { it.copy(isLoading = false, chats = chatList) }
            }
        }
    }
    private fun summarizeChat(chatId: Long) {
        viewModelScope.launch {
            // Включаем крутилку лоадера для AI
            _state.update { it.copy(isSummarizing = true, errorMessage = null) }

            val result = generateChatSummaryUseCase(chatId = chatId)

            result.onSuccess { summary ->
                _state.update { it.copy(isSummarizing = false, aiSummaryText = summary) }
            }.onFailure { error ->
                _state.update { it.copy(isSummarizing = false, errorMessage = error.message ?: "Ошибка выжимки") }
            }
        }
    }

    private fun dismissSummary() {
        _state.update { it.copy(aiSummaryText = null, errorMessage = null) }
    }
}