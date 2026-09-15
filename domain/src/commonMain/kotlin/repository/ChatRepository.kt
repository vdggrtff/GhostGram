package repository

import entity.Chat
import entity.Message
import entity.MyProfile
import entity.PublicChat
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeChats(): Flow<List<Chat>>
    fun observeChat(chatId: Long): Flow<Chat?>
    fun observeMessages(chatId: Long): Flow<List<Message>>
    suspend fun sendMessage(chatId: Long, text: String, )

    suspend fun sendMessage(chatId: Long, text: String, useCrypto: Boolean = false)
    suspend fun getChatHistory(chatId: Long, limit: Int): String // Для ИИ

    fun observeGhostMode(): Flow<Boolean>
    fun toggleGhostMode()
    fun markChatAsRead(chatId: Long, messageIds: List<Long>)

    suspend fun loadMoreMessages(chatId: Long, fromMessageId: Long)

    suspend fun requestKeyExchange(chatId: Long)

    fun observeMyProfile(): Flow<MyProfile>

    fun observeSearchResults(): Flow<List<PublicChat>>
    fun searchPublicChats(query: String)

    fun observeMessageSearchResults(): Flow<List<Chat>>
    fun searchMessages(query: String)
}