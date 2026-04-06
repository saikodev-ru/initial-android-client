package ru.saikodev.initial.data.api.dto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatDto(
    val chat_id: Int = 0,
    val partner_id: Int? = null,
    val partner_name: String? = null,
    val partner_signal_id: String? = null,
    val partner_avatar: String? = null,
    val partner_last_seen: Long? = null,
    val partner_is_typing: Int? = null,
    val partner_is_system: Boolean = false,
    val partner_is_verified: Boolean = false,
    val partner_is_team_signal: Boolean = false,
    val partner_bio: String? = null,
    val is_saved_msgs: Boolean = false,
    val is_pinned: Boolean = false,
    val pin_order: Int? = null,
    val is_muted: Boolean = false,
    val is_protected: Boolean = false,
    val unread_count: Int = 0,
    val last_message: String? = null,
    val last_time: Long? = null,
    val last_media_type: String? = null,
    val last_sender_id: Int? = null,
    val is_read: Int? = null
)

@Serializable
data class MessageDto(
    val id: Int = 0,
    val chat_id: Int? = null,
    val sender_id: Int? = null,
    val body: String? = null,
    val sent_at: Long? = null,
    val is_read: Int? = null,
    val is_edited: Int? = null,
    val is_deleted: Int? = null,
    val nickname: String? = null,
    val avatar_url: String? = null,
    val reply_to: Int? = null,
    val media_url: String? = null,
    val media_type: String? = null, // "image", "video", "voice", "document"
    val media_spoiler: Int? = null,
    val media_file_name: String? = null,
    val media_file_size: Long? = null,
    val media_file_ext: String? = null,
    val batch_id: String? = null,
    val reactions: List<ReactionDto>? = null,
    val patch_only: Boolean? = null
)

@Serializable
data class ReactionDto(
    val emoji: String = "",
    val count: Int = 0,
    val by_me: Boolean = false
)

@Serializable
data class GetMessagesResponse(
    val ok: Boolean = false,
    val messages: List<MessageDto>? = null,
    val chats: List<ChatDto>? = null,
    val deleted_ids: List<Int>? = null,
    val last_read_id: Int? = null,
    val has_more: Boolean? = null
)

@Serializable
data class SendMessageRequest(
    val to_signal_id: String? = null,
    val body: String? = null,
    val reply_to: Int? = null,
    val media_url: String? = null,
    val media_type: String? = null,
    val media_spoiler: Int? = null,
    val media_file_name: String? = null,
    val media_file_size: Long? = null,
    val batch_id: String? = null
)

@Serializable
data class SendMessageResponse(
    val ok: Boolean = false,
    val message_id: Int? = null,
    val chat_id: Int? = null,
    val media_url: String? = null,
    val message: String? = null
)

@Serializable
data class EditMessageRequest(val message_id: Int, val body: String)

@Serializable
data class EditMessageResponse(val ok: Boolean = false, val message: String? = null)

@Serializable
data class DeleteMessageRequest(val message_id: Int)

@Serializable
data class DeleteMessageResponse(val ok: Boolean = false, val message: String? = null)

@Serializable
data class ReactMessageRequest(val message_id: Int, val emoji: String)

@Serializable
data class ReactMessageResponse(val ok: Boolean = false, val reactions: List<ReactionDto>? = null)

@Serializable
data class SearchUserResponse(
    val ok: Boolean = false,
    val results: List<UserDto>? = null
)

@Serializable
data class PinChatRequest(
    val chat_id: Int,
    val pin: Boolean? = null,
    val reorder: List<Int>? = null
)

@Serializable
data class PinChatResponse(val ok: Boolean = false, val message: String? = null)

@Serializable
data class MuteChatRequest(val chat_id: Int)

@Serializable
data class MuteChatResponse(
    val ok: Boolean = false,
    val is_muted: Boolean? = null,
    val message: String? = null
)

@Serializable
data class DeleteChatResponse(val ok: Boolean = false, val message: String? = null)

@Serializable
data class UploadMediaResponse(
    val ok: Boolean = false,
    val url: String? = null,
    val media_type: String? = null,
    val message: String? = null
)

@Serializable
data class GetReactionsResponse(
    val ok: Boolean = false,
    val reactions: Map<Int, List<ReactionDto>>? = null
)

@Serializable
data class LinkPreviewResponse(
    val ok: Boolean = false,
    val url: String? = null,
    val title: String? = null,
    val description: String? = null,
    val image: String? = null,
    val domain: String? = null,
    val site_name: String? = null,
    val embed_type: String? = null
)

@Serializable
data class GetMeResponse(val ok: Boolean = false, val user: UserDto? = null)

@Serializable
data class SessionsResponse(
    val ok: Boolean = false,
    val sessions: List<SessionDto>? = null
)

@Serializable
data class SessionDto(
    val id: String? = null,
    val device: String? = null,
    val ip: String? = null,
    val last_active: Long? = null,
    val is_current: Boolean? = null
)

@Serializable
data class UpdatePresenceResponse(val ok: Boolean = false)

@Serializable
data class ApiError(
    val ok: Boolean = false,
    val error: String? = null,
    val message: String? = null
)
