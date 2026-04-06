package ru.saikodev.initial.data.repository

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import ru.saikodev.initial.data.api.InitialApi
import ru.saikodev.initial.data.api.dto.*
import ru.saikodev.initial.data.preferences.TokenManager
import ru.saikodev.initial.domain.model.User
import ru.saikodev.initial.domain.repository.AuthRepository
import ru.saikodev.initial.util.MediaUtils
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: InitialApi,
    private val tokenManager: TokenManager,
    private val json: Json
) : AuthRepository {

    override val isLoggedIn: Boolean get() = tokenManager.getToken() != null

    override suspend fun sendCode(email: String): Result<String> = try {
        val res = api.sendCode(SendCodeRequest(email))
        if (res.ok) Result.success(res.via ?: "email")
        else Result.failure(Exception(res.message ?: "Ошибка отправки кода"))
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun verifyCode(email: String, code: String): Result<User> = try {
        val res = api.verifyCode(VerifyCodeRequest(email, code))
        if (res.ok && res.token != null && res.user != null) {
            tokenManager.saveToken(res.token)
            tokenManager.saveUserJson(json.encodeToString(res.user))
            Result.success(res.user.toDomain())
        } else {
            Result.failure(Exception(res.message ?: "Ошибка верификации"))
        }
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun createProfile(nickname: String, signalId: String?): Result<User> = try {
        val res = api.updateProfile(UpdateProfileRequest(nickname = nickname, signal_id = signalId))
        if (res.ok) {
            val me = api.getMe()
            if (me.ok && me.user != null) {
                tokenManager.saveUserJson(json.encodeToString(me.user))
                Result.success(me.user.toDomain())
            } else Result.failure(Exception("Ошибка получения профиля"))
        } else Result.failure(Exception(res.message ?: "Ошибка создания профиля"))
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun getMe(): Result<User> = try {
        val res = api.getMe()
        if (res.ok && res.user != null) {
            tokenManager.saveUserJson(json.encodeToString(res.user))
            Result.success(res.user.toDomain())
        } else Result.failure(Exception(res.message ?: "Ошибка"))
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun qrCreate(): Result<QrCreateResponse> = try {
        val res = api.qrCreate()
        if (res.ok) Result.success(res) else Result.failure(Exception(res.message ?: "Ошибка QR"))
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun qrPoll(token: String): Result<QrPollResponse> = try {
        Result.success(api.qrPoll(token))
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun approveQr(qrToken: String): Result<Unit> = try {
        val res = api.qrApprove(QrApproveRequest(qrToken))
        if (res.ok) Result.success(Unit) else Result.failure(Exception(res.message ?: "Ошибка"))
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun consumeQrLink(token: String): Result<User> = try {
        val res = api.qrLinkConsume(QrLinkConsumeRequest(token))
        if (res.ok && res.auth_token != null && res.user != null) {
            tokenManager.saveToken(res.auth_token)
            tokenManager.saveUserJson(json.encodeToString(res.user))
            Result.success(res.user.toDomain())
        } else Result.failure(Exception(res.message ?: "Ошибка QR-ссылки"))
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun saveQrAuth(token: String, userJson: String) {
        tokenManager.saveToken(token)
        tokenManager.saveUserJson(userJson)
    }

    override suspend fun logout() { tokenManager.clearAll() }

    override fun getSavedUser(): User? {
        val userJson = tokenManager.getUserJson() ?: return null
        return try { Json.decodeFromString<UserDto>(userJson).toDomain() }
        catch (_: Exception) { null }
    }

    override suspend fun resendCode(email: String, forceEmail: Boolean): Result<String> = try {
        val res = api.sendCode(SendCodeRequest(email, force_email = forceEmail))
        if (res.ok) Result.success(res.via ?: "email")
        else Result.failure(Exception(res.message ?: "Ошибка"))
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun getSessions(): Result<SessionsResponse> = try {
        Result.success(api.getSessions())
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun updateProfile(nickname: String?, signalId: String?, bio: String?): Result<User> = try {
        val res = api.updateProfile(UpdateProfileRequest(nickname = nickname, signal_id = signalId, bio = bio))
        if (res.ok) {
            val me = api.getMe()
            if (me.ok && me.user != null) {
                tokenManager.saveUserJson(json.encodeToString(me.user))
                Result.success(me.user.toDomain())
            } else Result.failure(Exception("Ошибка получения профиля"))
        } else Result.failure(Exception(res.message ?: "Ошибка обновления профиля"))
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun uploadAvatar(filePath: String): Result<String> = try {
        val file = File(filePath)
        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
        val res = api.uploadAvatar(body)
        if (res.ok && res.url != null) Result.success(res.url)
        else Result.failure(Exception(res.message ?: "Ошибка загрузки аватара"))
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun terminateSession(sessionId: String): Result<Unit> = try {
        val res = api.terminateSession(sessionId)
        if (res.ok) Result.success(Unit)
        else Result.failure(Exception(res.message ?: "Ошибка"))
    } catch (e: Exception) { Result.failure(e) }
}

private fun UserDto.toDomain() = User(
    id = id, email = email, nickname = nickname, signalId = signal_id,
    avatarUrl = resolveMediaUrl(avatar_url), bio = bio,
    isVerified = (is_verified ?: 0) == 1, isTeamSignal = (is_team_signal ?: 0) == 1
)

private fun resolveMediaUrl(url: String?): String? {
    if (url == null) return null
    if (url.startsWith("http") || url.startsWith("data:") || url.startsWith("blob:")) return url
    val token = "" // Will be resolved by MediaUtils at display time
    return MediaUtils.getMediaUrl(url, token)
}
