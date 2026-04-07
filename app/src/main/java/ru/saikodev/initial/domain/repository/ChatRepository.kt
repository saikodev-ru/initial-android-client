package ru.saikodev.initial.domain.repository

import ru.saikodev.initial.domain.model.*

interface ChatRepository {
    suspend fun loadChats(): Result<List<Chat>>
    suspend fun getMessages(chatId: Int, init: Boolean, afterId: Int?): Result<MessagesResult>
    suspend fun loadHistory(chatId: Int, beforeId: Int): Result<MessagesResult>
    suspend fun sendMessage(toSignalId: String, body: String?, replyTo: Int?): Result<SendMessageResult>
    suspend fun sendMediaMessage(toSignalId: String, mediaUrl: String, mediaType: String, caption: String?, replyTo: Int?, spoiler: Boolean, batchId: String?): Result<SendMessageResult>
    suspend fun editMessage(messageId: Int, newBody: String): Result<Unit>
    suspend fun deleteMessage(messageId: Int): Result<Unit>
    suspend fun toggleReaction(messageId: Int, emoji: String): Result<List<Reaction>>
    suspend fun searchUsers(query: String): Result<List<User>>
    suspend fun pinChat(chatId: Int, pin: Boolean): Result<Unit>
    suspend fun muteChat(chatId: Int): Result<Boolean>
    suspend fun deleteChat(chatId: Int): Result<Unit>
    suspend fun uploadMedia(filePath: String): Result<UploadResult>
    suspend fun uploadFile(filePath: String): Result<UploadResult>
    suspend fun updatePresence()
    suspend fun getLinkPreview(url: String): Result<LinkPreview>
    suspend fun registerFcmToken(fcmToken: String): Result<Unit>
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
