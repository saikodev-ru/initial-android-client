package ru.saikodev.initial.data.repository

import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import ru.saikodev.initial.data.api.InitialApi
import ru.saikodev.initial.data.api.dto.*
import ru.saikodev.initial.data.preferences.TokenManager
import ru.saikodev.initial.domain.model.*
import ru.saikodev.initial.domain.repository.ChatRepository
import ru.saikodev.initial.domain.repository.MessagesResult
import ru.saikodev.initial.domain.repository.SendMessageResult
import ru.saikodev.initial.domain.repository.UploadResult
import ru.saikodev.initial.util.MediaUtils
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val api: InitialApi,
    private val tokenManager: TokenManager
) : ChatRepository {

    private val token: String get() = tokenManager.getToken() ?: ""

    override suspend fun loadChats(): Result<List<Chat>> = try {
        Log.d("ChatRepo", "Loading chats...")
        val res = api.getMessages(chatId = 0)
        Log.d("ChatRepo", "Response ok=${res.ok}, chats=${res.chats?.size}")
        if (res.ok) {
            val chats = (res.chats ?: emptyList()).map { it.toDomain() }
                .sortedWith(compareByDescending<Chat> { it.isPinned }.thenByDescending { it.lastTime ?: 0 })
            Result.success(chats)
        } else {
            Result.failure(Exception(res.message ?: "Ошибка загрузки чатов"))
        }
    } catch (e: Exception) {
        Log.e("ChatRepo", "loadChats failed", e)
        Result.failure(e)
    }

    override suspend fun getMessages(chatId: Int, init: Boolean, afterId: Int?): Result<MessagesResult> = try {
        val res = api.getMessages(
            chatId = chatId, init = if (init) 1 else null, limit = 50,
            afterId = afterId, markRead = 1
        )
        if (res.ok) {
            Result.success(MessagesResult(
                messages = (res.messages ?: emptyList()).map { it.toDomain() },
                chats = res.chats?.map { it.toDomain() },
                deletedIds = res.deleted_ids ?: emptyList(),
                lastReadId = res.last_read_id
            ))
        } else Result.failure(Exception(res.message ?: "Ошибка"))
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun loadHistory(chatId: Int, beforeId: Int): Result<MessagesResult> = try {
        val res = api.getMessages(chatId = chatId, beforeId = beforeId, limit = 50, skipChats = 1)
        if (res.ok) {
            Result.success(MessagesResult(
                messages = (res.messages ?: emptyList()).map { it.toDomain() },
                chats = null, deletedIds = emptyList(), lastReadId = null
            ))
        } else Result.failure(Exception(res.message ?: "Ошибка"))
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun sendMessage(toSignalId: String, body: String?, replyTo: Int?): Result<SendMessageResult> = try {
        val res = api.sendMessage(SendMessageRequest(to_signal_id = toSignalId, body = body, reply_to = replyTo))
        if (res.ok) Result.success(SendMessageResult(res.message_id ?: 0, res.chat_id, res.media_url))
        else Result.failure(Exception(res.message ?: "Ошибка отправки"))
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun sendMediaMessage(
        toSignalId: String, mediaUrl: String, mediaType: String,
        caption: String?, replyTo: Int?, spoiler: Boolean, batchId: String?
    ): Result<SendMessageResult> = try {
        val res = api.sendMessage(SendMessageRequest(
            to_signal_id = toSignalId, body = caption, reply_to = replyTo,
            media_url = mediaUrl, media_type = mediaType,
            media_spoiler = if (spoiler) 1 else null, batch_id = batchId
        ))
        if (res.ok) Result.success(SendMessageResult(res.message_id ?: 0, res.chat_id, res.media_url))
        else Result.failure(Exception(res.message ?: "Ошибка отправки"))
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun editMessage(messageId: Int, newBody: String): Result<Unit> = try {
        val res = api.editMessage(EditMessageRequest(messageId, newBody))
        if (res.ok) Result.success(Unit) else Result.failure(Exception(res.message ?: "Ошибка"))
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun deleteMessage(messageId: Int): Result<Unit> = try {
        val res = api.deleteMessage(DeleteMessageRequest(messageId))
        if (res.ok) Result.success(Unit) else Result.failure(Exception(res.message ?: "Ошибка"))
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun toggleReaction(messageId: Int, emoji: String): Result<List<Reaction>> = try {
        val res = api.reactMessage(ReactMessageRequest(messageId, emoji))
        if (res.ok) Result.success((res.reactions ?: emptyList()).map { it.toDomain() })
        else Result.failure(Exception(res.message ?: "Ошибка"))
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun searchUsers(query: String): Result<List<User>> = try {
        val res = api.searchUser(query)
        if (res.ok) Result.success((res.users ?: emptyList()).map { it.toDomain() })
        else Result.failure(Exception(res.message ?: "Ошибка поиска"))
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun pinChat(chatId: Int, pin: Boolean): Result<Unit> = try {
        val res = api.pinChat(PinChatRequest(chatId, pin = pin))
        if (res.ok) Result.success(Unit) else Result.failure(Exception(res.message ?: "Ошибка"))
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun muteChat(chatId: Int): Result<Boolean> = try {
        val res = api.muteChat(MuteChatRequest(chatId))
        if (res.ok) Result.success((res.is_muted ?: 0) == 1)
        else Result.failure(Exception(res.message ?: "Ошибка"))
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun deleteChat(chatId: Int): Result<Unit> = try {
        val res = api.deleteChat(DeleteChatRequest(chatId))
        if (res.ok) Result.success(Unit) else Result.failure(Exception(res.message ?: "Ошибка"))
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun uploadMedia(filePath: String): Result<UploadResult> = try {
        val file = File(filePath)
        val requestFile = file.asRequestBody(MediaUtils.getMimeType(file).toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
        val res = api.uploadMedia(body)
        if (res.ok && res.url != null) Result.success(UploadResult(res.url, res.media_type))
        else Result.failure(Exception(res.message ?: "Ошибка загрузки"))
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun uploadFile(filePath: String): Result<UploadResult> = try {
        val file = File(filePath)
        val requestFile = file.asRequestBody(MediaUtils.getMimeType(file).toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
        val res = api.uploadFile(body)
        if (res.ok && res.url != null) Result.success(UploadResult(res.url, "document"))
        else Result.failure(Exception(res.message ?: "Ошибка загрузки"))
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun updatePresence() { try { api.updatePresence() } catch (_: Exception) {} }

    override suspend fun getLinkPreview(url: String): Result<LinkPreview> = try {
        val res = api.getLinkPreview(url)
        if (res.ok) Result.success(LinkPreview(
            url = res.url ?: url, title = res.title, description = res.description,
            image = res.image, domain = res.domain, siteName = res.site_name, embedType = res.embed_type
        )) else Result.failure(Exception("Ошибка"))
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun registerFcmToken(fcmToken: String): Result<Unit> = try {
        val res = api.registerFcm(RegisterFcmRequest(fcm_token = fcmToken))
        if (res.ok) Result.success(Unit)
        else Result.failure(Exception(res.message ?: "Ошибка регистрации FCM"))
    } catch (e: Exception) { Result.failure(e) }
}

// ── DTO → Domain mapping ──

private fun ChatDto.toDomain() = Chat(
    chatId = chat_id, partnerId = partner_id, partnerName = partner_name,
    partnerSignalId = partner_signal_id, partnerAvatar = MediaUtils.resolveUrl(partner_avatar),
    partnerLastSeen = partner_last_seen, partnerIsTyping = (partner_is_typing ?: 0) == 1,
    partnerIsSystem = (partner_is_system ?: 0) == 1, partnerIsVerified = (partner_is_verified ?: 0) == 1,
    partnerIsTeamSignal = (partner_is_team_signal ?: 0) == 1, partnerBio = partner_bio,
    isSavedMsgs = (is_saved_msgs ?: 0) == 1, isPinned = (is_pinned ?: 0) == 1, pinOrder = pin_order,
    isMuted = (is_muted ?: 0) == 1, isProtected = (is_protected ?: 0) == 1,
    unreadCount = unread_count, lastMessage = last_message, lastTime = last_time,
    lastMediaType = last_media_type, lastSenderId = last_sender_id, isRead = (is_read ?: 0) == 1
)

private fun MessageDto.toDomain() = Message(
    id = id, chatId = chat_id, senderId = sender_id, body = body, sentAt = sent_at,
    isRead = (is_read ?: 0) == 1, isEdited = (is_edited ?: 0) == 1, isDeleted = (is_deleted ?: 0) == 1,
    nickname = nickname, avatarUrl = MediaUtils.resolveUrl(avatar_url), replyTo = reply_to,
    replyBody = reply_body, replyNickname = reply_nickname,
    mediaUrl = MediaUtils.resolveUrl(media_url), mediaType = media_type,
    mediaSpoiler = (media_spoiler ?: 0) == 1, mediaFileName = media_file_name,
    mediaFileSize = media_file_size, mediaFileExt = media_file_ext,
    batchId = batch_id, voiceDuration = voice_duration,
    reactions = (reactions ?: emptyList()).map { it.toDomain() }
)

private fun ReactionDto.toDomain() = Reaction(emoji, count, by_me)

private fun UserDto.toDomain() = User(
    id = id, email = email, nickname = nickname, signalId = signal_id,
    avatarUrl = MediaUtils.resolveUrl(avatar_url), bio = bio,
    isVerified = (is_verified ?: 0) == 1, isTeamSignal = (is_team_signal ?: 0) == 1
)
