package usecase

import repository.AiRepository
import repository.ChatRepository

/**
 * Юзкейс: Сгенерировать краткую выжимку (Summary) для конкретного чата.
 */
class GenerateChatSummaryUseCase(
    private val chatRepository: ChatRepository,
    private val aiRepository: AiRepository
) {
    suspend operator fun invoke(chatId: Long, limit: Int = 100): Result<String> {
        return try {
            // 1. Просим репозиторий чатов дать нам историю (последние N сообщений)
            val chatHistory = chatRepository.getChatHistory(chatId = chatId, limit = limit)

            if (chatHistory.isBlank()){
                return Result.failure(Exception("Чат пуст, нечего суммаризировать."))
            }

            val summaryResult = aiRepository.getChatSummary(chatHistory)

            summaryResult
        } catch (e: Exception){
            Result.failure(e)
        }
    }
}