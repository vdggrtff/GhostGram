package repository

import entity.Chat
import kotlinx.coroutines.flow.Flow

interface ChatRepository {

    // Получаем список чатов (Flow, чтобы обновлять UI в реальном времени)
    fun observeChats(): Flow<List<Chat>>

    // Получаем историю сообщений для ИИ-выжимки
    suspend fun getChatHistory(chatId: Long, limit: Int): String
}