package ru.saikodev.initial.data.repository

import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import ru.saikodev.initial.data.api.InitialApi
import ru.saikodev.initial.data.api.dto.*
import ru.saikodev.initial.data.preferences.TokenManager
import ru.saikodev.initial.domain.model.*
import ru.saikodev.initial.domain.repository.*
import ru.saikodev.initial.util.MediaUtils
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val api: InitialApi,
    private val tokenManager: TokenManager
) : ChatRepository {

    override suspend fun loadChats(): Result<List<Chat>> {
        return try {
            val res = api.getMessages(chatId = 0)
            if (res.ok) {
                Result.success((res.chats ?: emptyList()).map { it.toDomain() }.sortedWith(compareByDescending<Chat> { it.isPinned }.thenByDescending { it.lastTime ?: 0 }))
            } else {
                Result.failure(Exception(res.message ?: "Ошибка загрузки чатов"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMessages(chatId: Int, init: Boolean, afterId: Int?): Result<MessagesResult> {
        return try {
            val res = api.getMessages(
                chatId = chatId,
                init = if (init) 1 else null,
                limit = 50,
                afterId = afterId,
                markRead = 1
            )
            if (res.ok) {
                val token = tokenManager.getToken() ?: ""
                val messages = (res.messages ?: emptyList()).map { it.toDomain(token) }
                val chats = res.chats?.map { it.toDomain() }
                Result.success(MessagesResult(messages, chats, res.deleted_ids ?: emptyList(), res.last_read_id))
            } else {
                Result.failure(Exception(res.message ?: "Ошибка"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun loadHistory(chatId: Int, beforeId: Int): Result<MessagesResult> {
        return try {
            val res = api.getMessages(
                chatId = chatId,
                beforeId = beforeId,
                limit = 50,
                skipChats = 1
            )
            if (res.ok) {
                val token = tokenManager.getToken() ?: ""
                val messages = (res.messages ?: emptyList()).map { it.toDomain(token) }
                Result.success(MessagesResult(messages, null, emptyList(), null))
            } else {
                Result.failure(Exception(res.message ?: "Ошибка"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendMessage(toSignalId: String, body: String?, replyTo: Int?): Result<SendMessageResult> {
        return try {
            val res = api.sendMessage(SendMessageRequest(
                to_signal_id = toSignalId,
                body = body,
                reply_to = replyTo
            ))
            if (res.ok) {
                Result.success(SendMessageResult(res.message_id ?: 0, res.chat_id, res.media_url))
            } else {
                Result.failure(Exception(res.message ?: "Ошибка отправки"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendMediaMessage(
        toSignalId: String, mediaUrl: String, mediaType: String,
        caption: String?, replyTo: Int?, spoiler: Boolean, batchId: String?
    ): Result<SendMessageResult> {
        return try {
            val res = api.sendMessage(SendMessageRequest(
                to_signal_id = toSignalId,
                body = caption,
                reply_to = replyTo,
                media_url = mediaUrl,
                media_type = mediaType,
                media_spoiler = if (spoiler) 1 else null,
                batch_id = batchId
            ))
            if (res.ok) {
                Result.success(SendMessageResult(res.message_id ?: 0, res.chat_id, res.media_url))
            } else {
                Result.failure(Exception(res.message ?: "Ошибка отправки"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun editMessage(messageId: Int, newBody: String): Result<Unit> {
        return try {
            val res = api.editMessage(EditMessageRequest(messageId, newBody))
            if (res.ok) Result.success(Unit) else Result.failure(Exception(res.message ?: "Ошибка"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteMessage(messageId: Int): Result<Unit> {
        return try {
            val res = api.deleteMessage(DeleteMessageRequest(messageId))
            if (res.ok) Result.success(Unit) else Result.failure(Exception(res.message ?: "Ошибка"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleReaction(messageId: Int, emoji: String): Result<List<Reaction>> {
        return try {
            val res = api.reactMessage(ReactMessageRequest(messageId, emoji))
            if (res.ok) {
                Result.success((res.reactions ?: emptyList()).map { it.toDomain() })
            } else {
                Result.failure(Exception(res.message ?: "Ошибка"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchUsers(query: String): Result<List<User>> {
        return try {
            val res = api.searchUser(query)
            if (res.ok) {
                Result.success((res.users ?: emptyList()).map { it.toDomain() })
            } else {
                Result.failure(Exception(res.message ?: "Ошибка поиска"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun pinChat(chatId: Int, pin: Boolean): Result<Unit> {
        return try {
            val res = api.pinChat(PinChatRequest(chatId, pin = pin))
            if (res.ok) Result.success(Unit) else Result.failure(Exception(res.message ?: "Ошибка"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun muteChat(chatId: Int): Result<Boolean> {
        return try {
            val res = api.muteChat(MuteChatRequest(chatId))
            if (res.ok) Result.success(res.is_muted ?: false) else Result.failure(Exception(res.message ?: "Ошибка"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteChat(chatId: Int): Result<Unit> {
        return try {
            val res = api.deleteChat(DeleteChatRequest(chatId))
            if (res.ok) Result.success(Unit) else Result.failure(Exception(res.message ?: "Ошибка"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadMedia(filePath: String): Result<UploadResult> {
        return try {
            val file = File(filePath)
            val requestFile = file.asRequestBody(MediaUtils.getMimeType(file).toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
            val res = api.uploadMedia(body)
            if (res.ok && res.url != null) {
                Result.success(UploadResult(res.url, res.media_type))
            } else {
                Result.failure(Exception(res.message ?: "Ошибка загрузки"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadFile(filePath: String): Result<UploadResult> {
        return try {
            val file = File(filePath)
            val requestFile = file.asRequestBody(MediaUtils.getMimeType(file).toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
            val res = api.uploadFile(body)
            if (res.ok && res.url != null) {
                Result.success(UploadResult(res.url, "document"))
            } else {
                Result.failure(Exception(res.message ?: "Ошибка загрузки"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePresence() {
        try { api.updatePresence() } catch (_: Exception) {}
    }

    override suspend fun getLinkPreview(url: String): Result<LinkPreview> {
        return try {
            val res = api.getLinkPreview(url)
            if (res.ok) {
                Result.success(LinkPreview(
                    url = res.url ?: url,
                    title = res.title,
                    description = res.description,
                    image = res.image,
                    domain = res.domain,
                    siteName = res.site_name,
                    embedType = res.embed_type
                ))
            } else {
                Result.failure(Exception("Ошибка"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// Extensions for DTO -> Domain mapping
private fun ChatDto.toDomain() = Chat(
    chatId = chat_id,
    partnerId = partner_id,
    partnerName = partner_name,
    partnerSignalId = partner_signal_id,
    partnerAvatar = partner_avatar,
    partnerLastSeen = partner_last_seen,
    partnerIsTyping = (partner_is_typing ?: 0) == 1,
    partnerIsSystem = partner_is_system,
    partnerIsVerified = partner_is_verified,
    partnerIsTeamSignal = partner_is_team_signal,
    partnerBio = partner_bio,
    isSavedMsgs = is_saved_msgs,
    isPinned = is_pinned,
    pinOrder = pin_order,
    isMuted = is_muted,
    isProtected = is_protected,
    unreadCount = unread_count,
    lastMessage = last_message,
    lastTime = last_time,
    lastMediaType = last_media_type,
    lastSenderId = last_sender_id,
    isRead = is_read
)

private fun MessageDto.toDomain(token: String = "") = Message(
    id = id,
    chatId = chat_id,
    senderId = sender_id,
    body = body,
    sentAt = sent_at,
    isRead = (is_read ?: 0) == 1,
    isEdited = (is_edited ?: 0) == 1,
    isDeleted = (is_deleted ?: 0) == 1,
    nickname = nickname,
    avatarUrl = avatarUrl(avatar_url, token),
    replyTo = reply_to,
    mediaUrl = mediaUrl(media_url, token),
    mediaType = media_type,
    mediaSpoiler = (media_spoiler ?: 0) == 1,
    mediaFileName = media_file_name,
    mediaFileSize = media_file_size,
    mediaFileExt = media_file_ext,
    batchId = batch_id,
    reactions = (reactions ?: emptyList()).map { it.toDomain() }
)

private fun ReactionDto.toDomain() = Reaction(emoji, count, by_me)

private fun avatarUrl(url: String?, token: String): String? {
    if (url == null) return null
    if (url.startsWith("http") || url.startsWith("data:")) return url
    return MediaUtils.getMediaUrl(url, token)
}

private fun mediaUrl(url: String?, token: String): String? {
    if (url == null) return null
    if (url.startsWith("http") || url.startsWith("data:")) return url
    return MediaUtils.getMediaUrl(url, token)
}

private fun UserDto.toDomain() = User(
    id = id, email = email, nickname = nickname,
    signalId = signal_id, avatarUrl = avatar_url,
    bio = bio, isVerified = is_verified, isTeamSignal = is_team_signal
)
