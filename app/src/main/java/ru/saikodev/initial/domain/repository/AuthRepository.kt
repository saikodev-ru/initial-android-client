package ru.saikodev.initial.domain.repository

import ru.saikodev.initial.data.api.dto.*
import ru.saikodev.initial.domain.model.User

interface AuthRepository {
    val isLoggedIn: Boolean
    suspend fun sendCode(email: String): Result<String>
    suspend fun verifyCode(email: String, code: String): Result<User>
    suspend fun createProfile(nickname: String, signalId: String?): Result<User>
    suspend fun getMe(): Result<User>
    suspend fun qrCreate(): Result<QrCreateResponse>
    suspend fun qrPoll(token: String): Result<QrPollResponse>
    suspend fun approveQr(qrToken: String): Result<Unit>
    suspend fun consumeQrLink(token: String): Result<User>
    suspend fun saveQrAuth(token: String, userJson: String)
    suspend fun logout()
    fun getSavedUser(): User?
    suspend fun resendCode(email: String, forceEmail: Boolean): Result<String>
    suspend fun updateProfile(nickname: String?, signalId: String?, bio: String?): Result<User>
    suspend fun uploadAvatar(filePath: String): Result<String>
    suspend fun getSessions(): Result<SessionsResponse>
    suspend fun terminateSession(sessionId: String): Result<Unit>
}
