package ru.saikodev.initial.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Chat(
    val chatId: Int = 0,
    val partnerId: Int? = null,
    val partnerName: String? = null,
    val partnerSignalId: String? = null,
    val partnerAvatar: String? = null,
    val partnerLastSeen: Long? = null,
    val partnerIsTyping: Boolean = false,
    val partnerIsSystem: Boolean = false,
    val partnerIsVerified: Boolean = false,
    val partnerIsTeamSignal: Boolean = false,
    val partnerBio: String? = null,
    val isSavedMsgs: Boolean = false,
    val isPinned: Boolean = false,
    val pinOrder: Int? = null,
    val isMuted: Boolean = false,
    val isProtected: Boolean = false,
    val unreadCount: Int = 0,
    val lastMessage: String? = null,
    val lastTime: Long? = null,
    val lastMediaType: String? = null,
    val lastSenderId: Int? = null,
    val isRead: Boolean = false
) : Parcelable
