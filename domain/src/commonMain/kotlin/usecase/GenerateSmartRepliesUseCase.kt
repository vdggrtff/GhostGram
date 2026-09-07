package usecase

import SessionManager
import repository.AiRepository
import repository.ChatRepository

class GenerateSmartRepliesUseCase(
    private val sessionManager: SessionManager,
    private val aiRepository: AiRepository
) {
    suspend operator fun invoke(chatId: Long): Result<List<String>> {
        // Для быстрых ответов нам хватит контекста последних 10 сообщений
        val chatRepo = sessionManager.currentSession.value?.chatRepository
            ?: return Result.failure(Exception("Нет активного аккаунта"))
        val history = chatRepo.getChatHistory(chatId = chatId, limit = 10)
        if (history.isBlank()) return Result.failure(Exception("Чат пуст"))

        return aiRepository.getSmartReplies(history)
    }
}