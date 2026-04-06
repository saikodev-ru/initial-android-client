package ru.saikodev.initial.domain.model

data class User(
    val id: Int = 0,
    val email: String = "",
    val nickname: String = "",
    val signalId: String? = null,
    val avatarUrl: String? = null,
    val bio: String? = null,
    val isVerified: Boolean = false,
    val isTeamSignal: Boolean = false
)
