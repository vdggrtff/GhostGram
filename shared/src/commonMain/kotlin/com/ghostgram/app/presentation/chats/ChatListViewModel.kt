package com.ghostgram.app.presentation.chats

import SessionManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    private var sessionObserversJob: Job? = null // Слушает базу данных
    private var searchDebounceJob: Job? = null   // Таймер задержки при вводе текста

    init {
        viewModelScope.launch {
            sessionManager.currentSession.collect { session ->
                if (session != null) {
                    observeSessionData(session.chatRepository)
                } else {
                    _state.update { it.copy(chats = emptyList(), globalSearchResults = emptyList(), messageSearchResults = emptyList()) }
                }
            }
        }
    }

    private fun observeSessionData(repo: ChatRepository) {
        sessionObserversJob?.cancel() // Отменяем старые подписки, если сменился аккаунт

        sessionObserversJob = viewModelScope.launch {
            // 1. Слушаем список чатов
            launch {
                repo.observeChats().collect { chatList ->
                    _state.update { it.copy(isLoading = false, chats = chatList) }
                }
            }
            // 2. Слушаем результаты глобального поиска (каналы)
            launch {
                repo.observeSearchResults().collect { publicChats ->
                    _state.update { it.copy(globalSearchResults = publicChats, isSearching = false) }
                }
            }
            // 3. Слушаем результаты поиска по сообщениям
            launch {
                repo.observeMessageSearchResults().collect { foundMessages ->
                    _state.update { it.copy(messageSearchResults = foundMessages, isSearching = false) }
                }
            }
        }
    }

    fun onIntent(intent: ChatListIntent) {
        val repo = sessionManager.currentSession.value?.chatRepository
        when (intent) {
            is ChatListIntent.LoadChats -> {
                repo?.let { observeSessionData(it) }
            }
            is ChatListIntent.OnSummarizeChatClick -> summarizeChat(intent.chatId)
            is ChatListIntent.OnDismissSummaryDialog -> dismissSummary()
            is ChatListIntent.OnSearchQueryChanged -> {
                _state.update { it.copy(searchQuery = intent.query) }

                searchDebounceJob?.cancel() // Отменяем таймер, если юзер продолжает печатать

                if (intent.query.isBlank()) {
                    // Если строка пустая — мгновенно очищаем поиск
                    repo?.searchPublicChats("")
                    repo?.searchMessages("")
                    _state.update { it.copy(globalSearchResults = emptyList(), messageSearchResults = emptyList(), isSearching = false) }
                } else {
                    _state.update { it.copy(isSearching = true) }

                    // 💥 Ждем 400мс и шлем ОДИН запрос
                    searchDebounceJob = viewModelScope.launch {
                        delay(400)
                        repo?.searchPublicChats(intent.query)
                        repo?.searchMessages(intent.query)
                    }
                }
            }
        }
    }

    private fun summarizeChat(chatId: Long) {
        viewModelScope.launch {
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