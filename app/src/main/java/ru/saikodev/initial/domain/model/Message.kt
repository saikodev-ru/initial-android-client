package ru.saikodev.initial.domain.model

data class Message(
    val id: Int = 0,
    val chatId: Int? = null,
    val senderId: Int? = null,
    val body: String? = null,
    val sentAt: Long? = null,
    val isRead: Boolean = false,
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false,
    val nickname: String? = null,
    val avatarUrl: String? = null,
    val replyTo: Int? = null,
    val mediaUrl: String? = null,
    val mediaType: String? = null, // "image", "video", "voice", "document"
    val mediaSpoiler: Boolean = false,
    val mediaFileName: String? = null,
    val mediaFileSize: Long? = null,
    val mediaFileExt: String? = null,
    val batchId: String? = null,
    val reactions: List<Reaction> = emptyList()
) {
    val isTemp: Boolean = false // temp IDs handled at repository level
    val isOutgoing: Boolean = false // determined by comparing with current user
}
