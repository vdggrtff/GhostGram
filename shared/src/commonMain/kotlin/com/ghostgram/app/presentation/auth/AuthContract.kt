package com.ghostgram.app.presentation.auth

enum class AuthStep {
    WaitPhoneNumber, WaitCode, WaitPassword, Authorized
}

data class AuthScreenState(
    val step: AuthStep = AuthStep.WaitPhoneNumber,
    val inputText: String = "", // Сюда юзер пишет номер, код или пароль
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface AuthIntent {
    data class OnInputChanged(val text: String) : AuthIntent
    data object OnSubmit : AuthIntent
    data object ClearError : AuthIntent
    data object OnCancelClick : AuthIntent
}