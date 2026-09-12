package com.ghostgram.app.presentation.settings

import AccountSession
import SessionManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsState(
    val accounts: List<AccountSession> = emptyList(),
    val currentAccountId: String? = null,
    val userName: String = "Алексей",
    val userHandle: String = "@alex_ghost",
    val phoneNumber: String = "+7 (999) 000-00-00",
    val avatarPath: String? = null,
    val isStealthMode: Boolean = true,
    val showAiDialog: Boolean = false,
    val aiApiKeyInput: String = "",
    val showStorageDialog: Boolean = false,
    val showCryptoDialog: Boolean = false
)

class SettingsViewModel(
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private var ghostModeJob: Job? = null
    private var avatarJob: Job? = null

    init {
        // 1. Слушаем список аккаунтов и текущую сессию
        viewModelScope.launch {
            combine(
                sessionManager.accounts,
                sessionManager.currentSession
            ) { accounts, current ->
                accounts to current
            }.collect { (accounts, current) ->
                _state.update {
                    it.copy(
                        accounts = accounts,
                        currentAccountId = current?.accountId
                    )
                }
                if (current != null) {
                    observeSessionData(current)
                }
            }
        }
    }

    private fun observeSessionData(session: AccountSession) {
        // Слушаем Ghost Mode текущего аккаунта
        ghostModeJob?.cancel()
        ghostModeJob = viewModelScope.launch {
            session.chatRepository.observeGhostMode().collect { isGhost ->
                _state.update { it.copy(isStealthMode = isGhost) }
            }
        }

        // Слушаем аватарку
        avatarJob?.cancel()
        avatarJob = viewModelScope.launch {
            session.chatRepository.observeMyProfile().collect { profile ->
                _state.update {
                    it.copy(
                        userName = profile.fullName,
                        userHandle = profile.handle,
                        phoneNumber = profile.phoneNumber, // Оставляем в стейте, но спрячем в UI
                        avatarPath = profile.avatarPath
                    )
                }
            }
        }
    }

    fun onToggleStealthMode() {
        sessionManager.currentSession.value?.chatRepository?.toggleGhostMode()
    }

    fun onAddAccount(onNavigateToAuth: () -> Unit) {
        sessionManager.addNewAccount()
        onNavigateToAuth()
    }

    fun onSwitchAccount(accountId: String, onNavigateToChatList: () -> Unit) {
        sessionManager.switchAccount(accountId)
        onNavigateToChatList()
    }

    fun onLogOut(onNavigateToAuth: () -> Unit, onNavigateToChatList: () -> Unit) {
        // Выходим из текущего ядра
        sessionManager.currentSession.value?.authRepository?.logOut()
        // Удаляем аккаунт из менеджера и файла
        sessionManager.removeCurrentAccount()

        if (sessionManager.currentSession.value == null) {
            // Если это был ПОСЛЕДНИЙ аккаунт - создаем новый пустой и кидаем на авторизацию
            sessionManager.addNewAccount()
            onNavigateToAuth()
        } else {
            // 💥 Если остались другие аккаунты - выкидываем юзера на список чатов нового активного профиля!
            onNavigateToChatList()
        }
    }

    fun setAiDialogOpen(isOpen: Boolean) = _state.update { it.copy(showAiDialog = isOpen) }
    fun updateAiKeyInput(key: String) = _state.update { it.copy(aiApiKeyInput = key) }
    fun saveAiKey() {
        // TODO: Сохранить ключ в DataStore (Пока просто закрываем диалог)
        _state.update { it.copy(showAiDialog = false, aiApiKeyInput = "") }
    }

    fun setStorageDialogOpen(isOpen: Boolean) = _state.update { it.copy(showStorageDialog = isOpen) }
    fun clearCache() {
        // TODO: Вызвать очистку кэша Room
        _state.update { it.copy(showStorageDialog = false) }
    }

    fun setCryptoDialogOpen(isOpen: Boolean) = _state.update { it.copy(showCryptoDialog = isOpen) }
}