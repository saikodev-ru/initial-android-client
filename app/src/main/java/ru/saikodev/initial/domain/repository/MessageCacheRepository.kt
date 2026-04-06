package ru.saikodev.initial.domain.repository

import ru.saikodev.initial.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface MessageCacheRepository {
    fun getCachedMessages(chatId: Int): Flow<List<Message>>
    suspend fun cacheMessages(chatId: Int, messages: List<Message>)
    suspend fun addMessage(chatId: Int, message: Message)
    suspend fun updateMessage(chatId: Int, message: Message)
    suspend fun deleteMessage(chatId: Int, messageId: Int)
    suspend fun deleteMessages(chatId: Int, messageIds: List<Int>)
}
