package repository

import entity.Chat
import entity.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeChats(): Flow<List<Chat>>
    fun observeMessages(chatId: Long): Flow<List<Message>>
    suspend fun sendMessage(chatId: Long, text: String)
    suspend fun getChatHistory(chatId: Long, limit: Int): String // Для ИИ
}