package ru.saikodev.initial.domain.repository

import ru.saikodev.initial.data.api.dto.QrCreateResponse
import ru.saikodev.initial.data.api.dto.QrPollResponse
import ru.saikodev.initial.domain.model.User

interface AuthRepository {
    val isLoggedIn: Boolean
    suspend fun sendCode(email: String): Result<Unit>
    suspend fun verifyCode(email: String, code: String): Result<User>
    suspend fun createProfile(nickname: String, signalId: String?): Result<User>
    suspend fun qrCreate(): Result<QrCreateResponse>
    suspend fun qrPoll(token: String): Result<QrPollResponse>
    suspend fun logout()
    fun getSavedUser(): User?
}
