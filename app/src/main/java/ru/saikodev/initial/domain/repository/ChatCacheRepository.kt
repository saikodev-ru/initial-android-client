package ru.saikodev.initial.domain.repository

import ru.saikodev.initial.domain.model.Chat
import kotlinx.coroutines.flow.Flow

interface ChatCacheRepository {
    fun getCachedChats(): Flow<List<Chat>>
    suspend fun cacheChats(chats: List<Chat>)
}
