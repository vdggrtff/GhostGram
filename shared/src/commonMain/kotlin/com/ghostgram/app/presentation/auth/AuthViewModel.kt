package com.ghostgram.app.presentation.auth

import SessionManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import entity.AuthState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import repository.AuthRepository

class AuthViewModel(
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(AuthScreenState())
    val state: StateFlow<AuthScreenState> = _state.asStateFlow()

    private var authJob: Job? = null

    init {
        // Подписываемся на обновления от Telegram ядра

        viewModelScope.launch {
            val accountsCount = sessionManager.accounts.value.size
            _state.update { it.copy(isFirstAccount = accountsCount <= 1) }
            sessionManager.currentSession.collect { session ->
                if (session != null) {
                    listenToAuth(session.authRepository)
                }
            }
        }
    }

    fun onIntent(intent: AuthIntent, onNavigateBack: () -> Unit = {}) {
        when (intent) {
            is AuthIntent.OnInputChanged -> _state.update { it.copy(inputText = intent.text, errorMessage = null) }
            is AuthIntent.ClearError -> _state.update { it.copy(errorMessage = null) }
            is AuthIntent.OnSubmit -> submitCurrentStep()
            is AuthIntent.OnCancelClick -> {
                sessionManager.cancelAddingAccount()
                onNavigateBack() // Возвращаемся в настройки
            }
        }
    }

    private fun listenToAuth(authRepository: AuthRepository) {
        authJob?.cancel()
        authJob = authRepository.observeAuthState()
            .onEach { domainState ->
                when (domainState) {
                    is AuthState.WaitPhoneNumber -> _state.update {
                        it.copy(step = AuthStep.WaitPhoneNumber, isLoading = false)
                    }
                    is AuthState.WaitCode -> _state.update {
                        it.copy(step = AuthStep.WaitCode, inputText = "", isLoading = false, errorMessage = null)
                    }
                    is AuthState.WaitPassword -> _state.update {
                        it.copy(step = AuthStep.WaitPassword, inputText = "", isLoading = false, errorMessage = null)
                    }
                    is AuthState.Authorized -> _state.update {
                        it.copy(step = AuthStep.Authorized, isLoading = false)
                    }
                    is AuthState.Error -> _state.update {
                        it.copy(errorMessage = domainState.message, isLoading = false)
                    }
                    is AuthState.Initial -> {}
                }
            }
            .launchIn(viewModelScope)
    }

    private fun submitCurrentStep() {
        val currentState = _state.value
        if (currentState.inputText.isBlank()) return

        val authRepo = sessionManager.currentSession.value?.authRepository ?: return

        _state.update { it.copy(isLoading = true) }

        when (currentState.step) {
            AuthStep.WaitPhoneNumber -> authRepo.sendPhoneNumber(currentState.inputText)
            AuthStep.WaitCode -> authRepo.sendAuthCode(currentState.inputText)
            AuthStep.WaitPassword -> authRepo.sendPassword(currentState.inputText)
            AuthStep.Authorized -> {}
        }
    }
}