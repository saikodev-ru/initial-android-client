package ru.saikodev.initial.data.api.dto

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
    val partner_is_system: Int? = null,
    val partner_is_verified: Int? = null,
    val partner_is_team_signal: Int? = null,
    val partner_bio: String? = null,
    val is_saved_msgs: Int? = null,
    val is_pinned: Int? = null,
    val pin_order: Int? = null,
    val is_muted: Int? = null,
    val is_protected: Int? = null,
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
    val reply_body: String? = null,
    val reply_nickname: String? = null,
    val media_url: String? = null,
    val media_type: String? = null,
    val media_spoiler: Int? = null,
    val media_file_name: String? = null,
    val media_file_size: Long? = null,
    val media_file_ext: String? = null,
    val batch_id: String? = null,
    val voice_duration: Int? = null,
    val reactions: List<ReactionDto>? = null,
    val patch_only: Int? = null
)

@Serializable
data class ReactionDto(val emoji: String = "", val count: Int = 0, val by_me: Boolean = false)

@Serializable
data class GetMessagesResponse(
    val ok: Boolean = false,
    val message: String? = null,
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
data class ReactMessageResponse(
    val ok: Boolean = false,
    val message: String? = null,
    val reactions: List<ReactionDto>? = null
)

@Serializable
data class GetReactionsResponse(val ok: Boolean = false, val reactions: Map<Int, List<ReactionDto>>? = null)

@Serializable
data class SearchUserResponse(val ok: Boolean = false, val message: String? = null, val users: List<UserDto>? = null)

@Serializable
data class PinChatRequest(val chat_id: Int, val pin: Boolean? = null, val reorder: List<Int>? = null)

@Serializable
data class PinChatResponse(val ok: Boolean = false, val message: String? = null)

@Serializable
data class MuteChatRequest(val chat_id: Int)

@Serializable
data class MuteChatResponse(val ok: Boolean = false, val is_muted: Int? = null, val message: String? = null)

@Serializable
data class DeleteChatRequest(val chat_id: Int)

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
data class UpdatePresenceResponse(val ok: Boolean = false)

@Serializable
data class CallSignalDto(
    val id: Int = 0,
    val sender_id: Int? = null,
    val target_id: Int? = null,
    val type: String? = null,
    val payload: String? = null,
    val created_at: Long? = null
)

@Serializable
data class CallSignalsResponse(
    val ok: Boolean = false,
    val signals: List<CallSignalDto>? = null,
    val last_id: Int? = null
)

@Serializable
data class RegisterFcmRequest(val fcm_token: String)

@Serializable
data class RegisterFcmResponse(val ok: Boolean = false, val message: String? = null)
