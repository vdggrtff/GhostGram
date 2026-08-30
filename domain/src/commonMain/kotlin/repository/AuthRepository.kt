package repository

import entity.AuthState
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun observeAuthState(): Flow<AuthState>
    fun sendPhoneNumber(phone: String)
    fun sendAuthCode(code: String)
    fun sendPassword(password: String)
}