package usecase

import repository.AiRepository
import repository.ChatRepository

class GenerateSmartRepliesUseCase(
    private val chatRepository: ChatRepository,
    private val aiRepository: AiRepository
) {
    suspend operator fun invoke(chatId: Long): Result<List<String>> {
        // Для быстрых ответов нам хватит контекста последних 10 сообщений
        val history = chatRepository.getChatHistory(chatId = chatId, limit = 10)
        if (history.isBlank()) return Result.failure(Exception("Чат пуст"))

        return aiRepository.getSmartReplies(history)
    }
}