package entity

sealed interface AuthState {
    data object Initial : AuthState
    data object WaitPhoneNumber : AuthState
    data object WaitCode : AuthState
    data object WaitPassword : AuthState // Для тех, у кого включена двухфакторка (2FA)
    data object Authorized : AuthState   // Ура, вошли!
    data class Error(val message: String) : AuthState
}