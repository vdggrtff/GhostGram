package com.ghostgram.app.presentation.settings

import AccountSession
import SessionManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class SettingsState(
    val accounts: List<AccountSession> = emptyList(),
    val currentAccountId: String? = null,
    val isStealthMode: Boolean = true
)

class SettingsViewModel(
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                sessionManager.accounts,
                sessionManager.currentSession
            ) { accounts, current ->
                SettingsState(
                    accounts = accounts,
                    currentAccountId = current?.accountId
                )
            }.collect { newState ->
                _state.value = newState
            }
        }
    }

    fun onAddAccount(onNavigateToAuth: () -> Unit) {
        // Создаем новую сессию для второго аккаунта
        sessionManager.addNewAccount()
        // Перекидываем на экран ввода номера для этого нового аккаунта!
        onNavigateToAuth()
    }

    fun onSwitchAccount(accountId: String) {
        sessionManager.switchAccount(accountId)
    }

    fun onLogOut(onNavigateToAuth: () -> Unit) {
        val current = sessionManager.currentSession.value

        // 1. Говорим Telegram выйти
        current?.authRepository?.logOut()

        // 2. Стираем из менеджера сессий
        sessionManager.removeCurrentAccount()

        // 3. Если аккаунтов больше не осталось — заводим новый чистый и отправляем на вход
        if (sessionManager.currentSession.value == null) {
            sessionManager.addNewAccount()
            onNavigateToAuth()
        }
    }
}