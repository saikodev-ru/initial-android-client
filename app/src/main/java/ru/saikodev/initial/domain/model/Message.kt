package ru.saikodev.initial.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
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
    val replyBody: String? = null,
    val replyNickname: String? = null,
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val mediaSpoiler: Boolean = false,
    val mediaFileName: String? = null,
    val mediaFileSize: Long? = null,
    val mediaFileExt: String? = null,
    val batchId: String? = null,
    val voiceDuration: Int? = null,
    val reactions: List<Reaction> = emptyList()
) : Parcelable
