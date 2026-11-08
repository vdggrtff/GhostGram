package com.ghostgram.data.repository.utils

import entity.Chat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ChatMemoryCache {
    private val _chatsMap = MutableStateFlow<Map<Long, Chat>>(emptyMap())
    val chatsFlow = _chatsMap.asStateFlow()

    val lastReadOutboxMap = mutableMapOf<Long, Long>()

    fun getChat(chatId: Long): Chat? = _chatsMap.value[chatId]

    fun getAllChats(): List<Chat> = _chatsMap.value.values.toList()

    fun updateAll(transform: (Map<Long, Chat>) -> Map<Long, Chat>) {
        _chatsMap.update(transform)
    }

    fun updateChat(chatId: Long, transform: (Chat) -> Chat) {
        _chatsMap.update { current ->
            val chat = current[chatId] ?: return@update current
            current + (chatId to transform(chat))
        }
    }

    fun putOrUpdate(chatId: Long, transform: (Chat?) -> Chat) {
        _chatsMap.update { current ->
            current + (chatId to transform(current[chatId]))
        }
    }
}