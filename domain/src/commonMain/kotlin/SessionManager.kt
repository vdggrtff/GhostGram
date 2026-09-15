import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import repository.AuthRepository
import repository.ChatRepository
import repository.ContactRepository
import kotlin.time.Clock
import kotlin.time.Clock.System

// Хранит все нужные классы для одного аккаунта
data class AccountSession(
    val accountId: String,
    val chatRepository: ChatRepository,
    val authRepository: AuthRepository,
    val contactRepository: ContactRepository
)

class SessionManager(
    private val appStorage: AppStorageConfig,
    private val sessionFactory: (String) -> AccountSession
) {
    private val _accounts = MutableStateFlow<List<AccountSession>>(emptyList())
    val accounts: StateFlow<List<AccountSession>> = _accounts.asStateFlow()

    private val _currentSession = MutableStateFlow<AccountSession?>(null)
    val currentSession: StateFlow<AccountSession?> = _currentSession.asStateFlow()

    //private val accountsFile = "ghostgram_accounts.json".toPath()
    private val accountsFile = "${appStorage.basePath}/ghostgram_accounts.json".toPath()

    fun loadSavedAccounts() {
        if (_accounts.value.isNotEmpty()) return

        val fs = FileSystem.SYSTEM
        if (fs.exists(accountsFile)) {
            try {
                val content = fs.read(accountsFile) { readUtf8() }

                // Поддержка старого и нового формата
                val parts = content.split("|")
                val currentId = if (parts.size == 2) parts[0] else ""
                val savedIdsString = if (parts.size == 2) parts[1] else parts[0]

                val savedIds = savedIdsString.split(",").filter { it.isNotBlank() }

                if (savedIds.isNotEmpty()) {
                    val sessions = savedIds.map { sessionFactory(it) }
                    _accounts.value = sessions

                    // 💥 ИЩЕМ ТОТ АККАУНТ, НА КОТОРОМ МЫ БЫЛИ В ПРОШЛЫЙ РАЗ
                    _currentSession.value = sessions.find { it.accountId == currentId } ?: sessions.firstOrNull()
                    println("💾 Загружены аккаунты. Активный: ${_currentSession.value?.accountId}")
                    return
                }
            } catch (e: Exception) { println("Ошибка загрузки аккаунтов") }
        }

        val defaultSession = sessionFactory("main_account")
        _accounts.value = listOf(defaultSession)
        _currentSession.value = defaultSession
        saveAccountsToDisk()
    }

    private fun saveAccountsToDisk() {
        val fs = FileSystem.SYSTEM
        val idsString = _accounts.value.joinToString(",") { it.accountId }
        val currentId = _currentSession.value?.accountId ?: ""

        // Формат: current_acc|acc1,acc2
        fs.write(accountsFile) { writeUtf8("$currentId|$idsString") }
    }

    fun addNewAccount() {
        val newAccountId = "acc_${Clock.System.now().toEpochMilliseconds()}"
        val newSession = sessionFactory(newAccountId)

        _accounts.value = _accounts.value + newSession
        _currentSession.value = newSession
        saveAccountsToDisk() // 💥 Сохраняем после добавления
    }

    fun removeCurrentAccount() {
        val current = _currentSession.value ?: return
        val remaining = _accounts.value.filter { it.accountId != current.accountId }

        _accounts.value = remaining
        _currentSession.value = remaining.firstOrNull()
        saveAccountsToDisk() // 💥 Сохраняем после удаления
    }

    fun switchAccount(accountId: String) {
        val session = _accounts.value.find { it.accountId == accountId }
        if (session != null) {
            _currentSession.value = session
            // 💥 ОБЯЗАТЕЛЬНО СОХРАНЯЕМ ВЫБОР НА ДИСК!
            saveAccountsToDisk()
            println("🔄 Переключились на: $accountId")
        }
    }

    fun cancelAddingAccount() {
        val pendingSession = _currentSession.value ?: return
        pendingSession.authRepository.logOut()
        removeCurrentAccount()
    }

}