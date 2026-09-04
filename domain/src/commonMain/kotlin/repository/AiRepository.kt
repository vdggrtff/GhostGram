package repository

interface AiRepository {

    // Отправляем историю чата нейросети и получаем выжимку (Summary)
    suspend fun getChatSummary(chatHistory: String): Result<String>
}