package usecase

import kotlinx.coroutines.flow.firstOrNull
import repository.AiRepository
import repository.ChatRepository

class GenerateCatchUpSummaryUseCase(
    private val chatRepository: ChatRepository,
    private val aiRepository: AiRepository
) {
    suspend operator fun invoke(chatId: Long): Result<String> {
        return try {
            // 1. Узнаем точное число непрочитанных сообщений в этом чате
            val chat = chatRepository.observeChat(chatId).firstOrNull()
            val unreadCount = chat?.unreadCount ?: 0

            // Если непрочитанных нет - берем последние 20, если есть - берем ровно число пропущенных (до 100)
            val limit = if (unreadCount > 0) unreadCount.coerceAtMost(100) else 20

            // 2. Достаем историю
            val history = chatRepository.getChatHistory(chatId = chatId, limit = limit)
            if (history.isBlank()) return Result.failure(Exception("Чат пуст"))

            // 3. Отправляем в Gemini
            aiRepository.getChatSummary(history)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}