package repository

import entity.Contact
import kotlinx.coroutines.flow.Flow

interface ContactRepository {
    fun observeContacts(): Flow<List<Contact>>
    suspend fun createPrivateChat(userId: Long): Long // Возвращает ID чата для перехода
}