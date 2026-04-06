package ru.saikodev.initial.domain.repository

import ru.saikodev.initial.domain.model.User

interface ProfileRepository {
    suspend fun getMe(): Result<User>
    suspend fun updateProfile(nickname: String?, signalId: String?, bio: String?): Result<Unit>
    suspend fun uploadAvatar(filePath: String): Result<String>
}
