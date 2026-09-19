package com.ghostgram.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ghostgram.core.database.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    // Сохраняем новые сообщения (если такое уже есть - заменяем)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    // Получаем историю чата (Flow сам обновит UI при добавлении в БД)
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY id ASC")
    fun observeMessages(chatId: Long): Flow<List<MessageEntity>>

    // 💥 МАГИЯ ANTI-REVOKE: Мы НЕ делаем DELETE. Мы делаем UPDATE!
    @Query("UPDATE messages SET isDeletedLocally = 1 WHERE id = :messageId AND chatId = :chatId")
    suspend fun markAsDeleted(chatId: Long, messageId: Long)

    @Query("UPDATE messages SET isRead = 1 WHERE chatId = :chatId AND id <= :lastReadOutboxId AND isOutgoing = 1")
    suspend fun markOutboxAsRead(chatId: Long, lastReadOutboxId: Long)

    @Query("DELETE FROM messages WHERE chatId = :chatId AND id = :messageId")
    suspend fun deleteMessage(chatId: Long, messageId: Long)

    // 💥 2. Обновляем текст, если сообщение отредактировали
    @Query("UPDATE messages SET text = :newText WHERE chatId = :chatId AND id = :messageId")
    suspend fun updateMessageText(chatId: Long, messageId: Long, newText: String)

    @Query("UPDATE messages SET photoPath = :path WHERE id = :messageId")
    suspend fun updateMessagePhoto(messageId: Long, path: String)

    @Query("UPDATE messages SET fileName = :path WHERE id = :messageId")
    suspend fun updateMessageFileName(messageId: Long, path: String)

    @Query("DELETE FROM messages WHERE isDeletedLocally = 0")
    suspend fun clearNormalMessages()

    // 💥 Удаляет ТОЛЬКО сохраненные Anti-Revoke сообщения
    @Query("DELETE FROM messages WHERE isDeletedLocally = 1")
    suspend fun clearAntiRevokeMessages()
}