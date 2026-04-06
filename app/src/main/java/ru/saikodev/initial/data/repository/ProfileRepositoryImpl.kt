package ru.saikodev.initial.data.repository

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import ru.saikodev.initial.data.api.InitialApi
import ru.saikodev.initial.data.api.dto.*
import ru.saikodev.initial.data.preferences.TokenManager
import ru.saikodev.initial.domain.model.User
import ru.saikodev.initial.domain.repository.ProfileRepository
import ru.saikodev.initial.util.MediaUtils
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val api: InitialApi,
    private val tokenManager: TokenManager
) : ProfileRepository {

    override suspend fun getMe(): Result<User> {
        return try {
            val token = tokenManager.getToken() ?: ""
            val res = api.getMe()
            if (res.ok && res.user != null) {
                val user = res.user
                tokenManager.saveUserJson(kotlinx.serialization.json.Json.encodeToString(UserDto.serializer(), user))
                Result.success(user.toDomain(token))
            } else {
                Result.failure(Exception("Ошибка получения профиля"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProfile(nickname: String?, signalId: String?, bio: String?): Result<Unit> {
        return try {
            val res = api.updateProfile(UpdateProfileRequest(nickname, signalId, bio))
            if (res.ok) Result.success(Unit) else Result.failure(Exception(res.message ?: "Ошибка"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadAvatar(filePath: String): Result<String> {
        return try {
            val file = File(filePath)
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
            val res = api.uploadAvatar(body)
            if (res.ok && res.url != null) {
                Result.success(res.url)
            } else {
                Result.failure(Exception(res.message ?: "Ошибка загрузки аватара"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

private fun resolveMediaUrl(url: String?, token: String): String? {
    if (url == null) return null
    if (url.startsWith("http") || url.startsWith("data:") || url.startsWith("blob:")) return url
    return MediaUtils.getMediaUrl(url, token)
}

private fun UserDto.toDomain(token: String = "") = User(
    id = id, email = email, nickname = nickname,
    signalId = signal_id, avatarUrl = resolveMediaUrl(avatar_url, token),
    bio = bio, isVerified = is_verified, isTeamSignal = is_team_signal
)
