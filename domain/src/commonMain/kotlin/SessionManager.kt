import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import repository.AuthRepository
import repository.ChatRepository
import kotlin.time.Clock
import kotlin.time.Clock.System

// Хранит все нужные классы для одного аккаунта
data class AccountSession(
    val accountId: String,
    val chatRepository: ChatRepository,
    val authRepository: AuthRepository
)

class SessionManager(
    private val sessionFactory: (String) -> AccountSession
) {
    private val _accounts = MutableStateFlow<List<AccountSession>>(emptyList())
    val accounts: StateFlow<List<AccountSession>> = _accounts.asStateFlow()

    private val _currentSession = MutableStateFlow<AccountSession?>(null)
    val currentSession: StateFlow<AccountSession?> = _currentSession.asStateFlow()

    // 💥 1. Создаем первый постоянный аккаунт
    fun initDefaultAccount() {
        if (_accounts.value.isNotEmpty()) return

        val defaultSession = sessionFactory("main_account") // Всегда одно имя! Сессия сохранится навсегда!
        _accounts.value = listOf(defaultSession)
        _currentSession.value = defaultSession
        println("✨ Инициализирован основной аккаунт: main_account")
    }

    fun removeCurrentAccount() {
        val current = _currentSession.value ?: return
        val remaining = _accounts.value.filter { it.accountId != current.accountId }

        _accounts.value = remaining
        _currentSession.value = remaining.firstOrNull()
        println("🗑️ Удален аккаунт: ${current.accountId}")
    }

    // 💥 2. А вот когда юзер САМ жмет "Добавить аккаунт", генерируем второй:
    fun addNewAccount() {
        val count = _accounts.value.size + 1
        val newAccountId = "account_$count" // Будет account_2, account_3 и т.д.
        val newSession = sessionFactory(newAccountId)

        _accounts.value = _accounts.value + newSession
        _currentSession.value = newSession
        println("✨ Добавлен новый аккаунт: $newAccountId")
    }

    fun switchAccount(accountId: String) {
        val session = _accounts.value.find { it.accountId == accountId }
        if (session != null) {
            _currentSession.value = session
            println("🔄 Переключились на: $accountId")
        }
    }
}