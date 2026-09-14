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

    private var searchResultsJob: Job? = null

    private var typingJob: Job? = null

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
            is ChatListIntent.OnSearchQueryChanged -> {
                println("📥 [2. ViewModel] Получен интент с текстом: '${intent.query}'")
                _state.update { it.copy(searchQuery = intent.query) }

                typingJob?.cancel()
                val session = sessionManager.currentSession.value
                val repo = session?.chatRepository

                if (repo == null) {
                    println("❌ [ViewModel ERROR] Репозиторий равен NULL! Активная сессия: ${session?.accountId}")
                    return
                }

                if (intent.query.isBlank()) {
                    repo.searchPublicChats("")
                    _state.update { it.copy(isSearching = false, globalSearchResults = emptyList()) }
                } else {
                    _state.update { it.copy(isSearching = true) }
                    typingJob = viewModelScope.launch {
                        kotlinx.coroutines.delay(400) // Ждем пока юзер допечатает
                        println("🚀 [2. ViewModel] Отправляем запрос в репозиторий: '${intent.query}'")
                        repo.searchPublicChats(intent.query)
                    }
                }
            }
        }
    }

    private fun loadChatsForSession(chatRepository: ChatRepository) {
        // Отменяем прослушивание старого аккаунта, если оно было
        currentChatsJob?.cancel()
        searchResultsJob?.cancel()

        _state.update { it.copy(isLoading = true) }

        currentChatsJob = viewModelScope.launch {
            chatRepository.observeChats().collect { chatList ->
                _state.update { it.copy(isLoading = false, chats = chatList) }
            }
        }

        searchResultsJob = viewModelScope.launch {
            chatRepository.observeSearchResults().collect { results ->
                _state.update { it.copy(globalSearchResults = results, isSearching = false) }
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