package ru.saikodev.initial.data.repository

import ru.saikodev.initial.data.api.InitialApi
import ru.saikodev.initial.data.api.dto.*
import ru.saikodev.initial.data.preferences.TokenManager
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.saikodev.initial.domain.model.User
import ru.saikodev.initial.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: InitialApi,
    private val tokenManager: TokenManager
) : AuthRepository {

    override val isLoggedIn: Boolean
        get() = tokenManager.getToken() != null

    override suspend fun sendCode(email: String): Result<String> {
        return try {
            val res = api.sendCode(SendCodeRequest(email))
            if (res.ok) {
                Result.success(res.via ?: "email")
            } else {
                Result.failure(Exception(res.message ?: "Ошибка отправки кода"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun verifyCode(email: String, code: String): Result<User> {
        return try {
            val res = api.verifyCode(VerifyCodeRequest(email, code))
            if (res.ok && res.token != null && res.user != null) {
                tokenManager.saveToken(res.token)
                tokenManager.saveUserJson(Json.encodeToString(res.user))
                Result.success(res.user.toDomain())
            } else {
                Result.failure(Exception(res.message ?: "Ошибка верификации"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createProfile(nickname: String, signalId: String?): Result<User> {
        return try {
            // First verify with empty code concept - use getMe after token is set
            val res = api.updateProfile(
                UpdateProfileRequest(nickname = nickname, signal_id = signalId)
            )
            if (res.ok) {
                val me = api.getMe()
                if (me.ok && me.user != null) {
                    tokenManager.saveUserJson(Json.encodeToString(me.user))
                    Result.success(me.user.toDomain())
                } else {
                    Result.failure(Exception("Ошибка получения профиля"))
                }
            } else {
                Result.failure(Exception(res.message ?: "Ошибка создания профиля"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun qrCreate(): Result<QrCreateResponse> {
        return try {
            val res = api.qrCreate()
            if (res.ok) Result.success(res)
            else Result.failure(Exception(res.message ?: "Ошибка создания QR"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun qrPoll(token: String): Result<QrPollResponse> {
        return try {
            val res = api.qrPoll(token)
            Result.success(res)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveQrAuth(token: String, userJson: String) {
        tokenManager.saveToken(token)
        tokenManager.saveUserJson(userJson)
    }

    override suspend fun logout() {
        tokenManager.clearAll()
    }

    override fun getSavedUser(): User? {
        val json = tokenManager.getUserJson() ?: return null
        return try {
            Json.decodeFromString<UserDto>(json).toDomain()
        } catch (e: Exception) {
            null
        }
    }
}

// Extension
private fun UserDto.toDomain() = User(
    id = id,
    email = email,
    nickname = nickname,
    signalId = signal_id,
    avatarUrl = avatar_url,
    bio = bio,
    isVerified = is_verified,
    isTeamSignal = is_team_signal
)
