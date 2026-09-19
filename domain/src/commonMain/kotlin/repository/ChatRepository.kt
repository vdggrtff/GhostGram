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

    suspend fun sendMessage(chatId: Long, text: String, useCrypto: Boolean = false, replyToMessageId: Long)
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

    suspend fun sendMedia(chatId: Long, bytes: ByteArray, extension: String, caption: String, useCrypto: Boolean, asDocument: Boolean, replyToMessageId: Long = 0L)

    suspend fun deleteMessage(chatId: Long, messageId: Long, revoke: Boolean)

    suspend fun clearLocalCache(clearNormal: Boolean, clearAntiRevoke: Boolean)

    suspend fun editMessageText(chatId: Long, messageId: Long, newText: String, useCrypto: Boolean = false)

}