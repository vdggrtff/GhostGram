package com.ghostgram.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import entity.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import repository.AuthRepository

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AuthScreenState())
    val state: StateFlow<AuthScreenState> = _state.asStateFlow()

    init {
        // Подписываемся на обновления от Telegram ядра
        authRepository.observeAuthState()
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

    fun onIntent(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.OnInputChanged -> _state.update { it.copy(inputText = intent.text, errorMessage = null) }
            is AuthIntent.ClearError -> _state.update { it.copy(errorMessage = null) }
            is AuthIntent.OnSubmit -> submitCurrentStep()
        }
    }

    private fun submitCurrentStep() {
        val currentState = _state.value
        if (currentState.inputText.isBlank()) return

        _state.update { it.copy(isLoading = true) }

        when (currentState.step) {
            AuthStep.WaitPhoneNumber -> authRepository.sendPhoneNumber(currentState.inputText)
            AuthStep.WaitCode -> authRepository.sendAuthCode(currentState.inputText)
            AuthStep.WaitPassword -> authRepository.sendPassword(currentState.inputText)
            AuthStep.Authorized -> {} // Ничего не делаем
        }
    }
}