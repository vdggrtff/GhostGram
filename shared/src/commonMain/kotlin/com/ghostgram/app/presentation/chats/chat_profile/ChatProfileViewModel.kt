package com.ghostgram.app.presentation.chats.chat_profile

import SessionManager
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import entity.ChatFullProfile
import entity.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatProfileState(
    val profile: ChatFullProfile? = null,
    val isLoading: Boolean = true,
    val selectedTab: Int = 0, // 0 = Медиа, 1 = Файлы, 2 = Голосовые
    val sharedMedia: List<Message> = emptyList(),
    val isLoadingMedia: Boolean = false,
    val isMuted: Boolean = false
)

class ChatProfileViewModel(
    savedStateHandle: SavedStateHandle,
    private val sessionManager: SessionManager
) : ViewModel() {

    val chatId: Long = savedStateHandle.get<Any>("chatId")?.toString()?.toLongOrNull() ?: 0L

    private val _state = MutableStateFlow(ChatProfileState())
    val state = _state.asStateFlow()

    init {
        loadProfile()
        loadMediaForTab(0)
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val repo = sessionManager.currentSession.value?.chatRepository
            val fullProfile = repo?.getChatFullProfile(chatId)
            _state.update { it.copy(profile = fullProfile, isLoading = false) }
        }
    }

    fun onTabSelected(index: Int) {
        _state.update { it.copy(selectedTab = index) }
        loadMediaForTab(index)
    }

    fun onToggleMute() {
        val currentMute = _state.value.isMuted
        val newMute = !currentMute
        _state.update { it.copy(isMuted = newMute) }

        viewModelScope.launch {
            val repo = sessionManager.currentSession.value?.chatRepository
            repo?.toggleChatMute(chatId, currentMute)
        }
    }

    private fun loadMediaForTab(tabIndex: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingMedia = true, sharedMedia = emptyList()) }
            val repo = sessionManager.currentSession.value?.chatRepository ?: return@launch

            // Выбираем фильтр TDLib в зависимости от вкладки
            val filterType = when (tabIndex) {
                0 -> "searchMessagesFilterPhotoAndVideo"
                1 -> "searchMessagesFilterDocument"
                2 -> "searchMessagesFilterVoiceNote"
                else -> "searchMessagesFilterPhotoAndVideo"
            }

            val items = repo.getSharedMedia(chatId, filterType)
            _state.update { it.copy(sharedMedia = items, isLoadingMedia = false) }
        }
    }
}