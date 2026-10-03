package repository

interface AiRepository {

    // Отправляем историю чата нейросети и получаем выжимку (Summary)
    suspend fun getChatSummary(chatHistory: String): Result<String>

    suspend fun getSmartReplies(chatHistory: String): Result<List<String>>

    suspend fun findSemanticMatches(query: String, chatHistory: String): Result<List<Long>>
}