package com.ghostgram.data.repository.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

data class CachedUserInfo(
    val name: String,
    val avatarPath: String?
)

class UserMemoryCache {
    private val _cache = MutableStateFlow<Map<Long, CachedUserInfo>>(emptyMap())

    fun getUser(userId: Long): CachedUserInfo? = _cache.value[userId]

    fun putUser(userId: Long, name: String, avatarPath: String?) {
        _cache.update { it + (userId to CachedUserInfo(name, avatarPath)) }
    }

    fun updateAvatar(userId: Long, avatarPath: String) {
        _cache.update { current ->
            val user = current[userId] ?: return@update current
            current + (userId to user.copy(avatarPath = avatarPath))
        }
    }
}