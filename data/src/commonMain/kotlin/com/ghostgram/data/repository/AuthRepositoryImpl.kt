package com.ghostgram.data.repository

import com.ghostgram.core.tdlib.TelegramFlowClient
import entity.AuthState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import repository.AuthRepository

class AuthRepositoryImpl(
    private val tdlibClient: TelegramFlowClient
) : AuthRepository {
    override fun observeAuthState(): Flow<AuthState> {
        return tdlibClient.updates.mapNotNull { json ->
            when {
                json.contains("authorizationStateWaitPhoneNumber") -> AuthState.WaitPhoneNumber
                json.contains("authorizationStateWaitCode") -> AuthState.WaitCode
                json.contains("authorizationStateWaitPassword") -> AuthState.WaitPassword
                json.contains("authorizationStateReady") -> AuthState.Authorized
                json.contains("error") && json.contains("PHONE_NUMBER_INVALID") ->
                    AuthState.Error("Неверный номер телефона")
                json.contains("error") && json.contains("PHONE_CODE_INVALID") ->
                    AuthState.Error("Неверный код подтверждения")
                else -> null
            }
        }
    }

    override fun sendPhoneNumber(phone: String) {
        val json = """
            {
                "@type": "setAuthenticationPhoneNumber",
                "phone_number": "$phone"
            }
        """.trimIndent()
        tdlibClient.send(json)
    }

    override fun sendAuthCode(code: String) {
        val json = """
            {
                "@type": "checkAuthenticationCode",
                "code": "$code"
            }
        """.trimIndent()
        tdlibClient.send(json)
    }

    override fun sendPassword(password: String) {
        val json = """
            {
                "@type": "checkAuthenticationPassword",
                "password": "$password"
            }
        """.trimIndent()
        tdlibClient.send(json)
    }
}