package ru.saikodev.initial.domain.repository

import ru.saikodev.initial.domain.model.Chat
import ru.saikodev.initial.domain.model.Message
import ru.saikodev.initial.domain.model.Reaction

interface ChatRepository {
    suspend fun loadChats(): Result<List<Chat>>
    suspend fun getMessages(chatId: Int, init: Boolean, afterId: Int? = null): Result<MessagesResult>
    suspend fun loadHistory(chatId: Int, beforeId: Int): Result<MessagesResult>
    suspend fun sendMessage(toSignalId: String, body: String?, replyTo: Int? = null): Result<SendMessageResult>
    suspend fun sendMediaMessage(toSignalId: String, mediaUrl: String, mediaType: String, caption: String?, replyTo: Int? = null, spoiler: Boolean = false, batchId: String? = null): Result<SendMessageResult>
    suspend fun editMessage(messageId: Int, newBody: String): Result<Unit>
    suspend fun deleteMessage(messageId: Int): Result<Unit>
    suspend fun toggleReaction(messageId: Int, emoji: String): Result<List<Reaction>>
    suspend fun searchUsers(query: String): Result<List<ru.saikodev.initial.domain.model.User>>
    suspend fun pinChat(chatId: Int, pin: Boolean): Result<Unit>
    suspend fun muteChat(chatId: Int): Result<Boolean>
    suspend fun deleteChat(chatId: Int): Result<Unit>
    suspend fun uploadMedia(filePath: String): Result<UploadResult>
    suspend fun uploadFile(filePath: String): Result<UploadResult>
    suspend fun updatePresence()
    suspend fun getLinkPreview(url: String): Result<ru.saikodev.initial.domain.model.LinkPreview>
}

data class MessagesResult(
    val messages: List<Message>,
    val chats: List<Chat>?,
    val deletedIds: List<Int>,
    val lastReadId: Int?
)

data class SendMessageResult(
    val messageId: Int,
    val chatId: Int?,
    val mediaUrl: String?
)

data class UploadResult(
    val url: String,
    val mediaType: String?
)
