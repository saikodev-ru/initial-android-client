package ru.saikodev.initial.data.api.dto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SendCodeRequest(val email: String, val force_email: Boolean? = null)

@Serializable
data class SendCodeResponse(
    val ok: Boolean = false,
    val message: String? = null,
    val via: String? = null // "email" or "signal"
)

@Serializable
data class VerifyCodeRequest(val email: String, val code: String)

@Serializable
data class VerifyCodeResponse(
    val ok: Boolean = false,
    val message: String? = null,
    val token: String? = null,
    val user: UserDto? = null
)

@Serializable
data class CreateProfileRequest(
    val nickname: String,
    val signal_id: String? = null,
    val bio: String? = null
)

@Serializable
data class UpdateProfileRequest(
    val nickname: String? = null,
    val signal_id: String? = null,
    val bio: String? = null
)

@Serializable
data class UserDto(
    val id: Int = 0,
    val email: String = "",
    val nickname: String = "",
    val signal_id: String? = null,
    val avatar_url: String? = null,
    val bio: String? = null,
    val is_verified: Boolean = false,
    val is_team_signal: Boolean = false
)

@Serializable
data class QrCreateResponse(
    val ok: Boolean = false,
    val token: String? = null,
    val url: String? = null,
    val message: String? = null
)

@Serializable
data class QrPollResponse(
    val ok: Boolean = false,
    val status: String? = null, // "scanned", "approved", "expired"
    val auth_token: String? = null,
    val user: UserDto? = null,
    val message: String? = null
)

@Serializable
data class QrApproveRequest(val qr_token: String)

@Serializable
data class QrApproveResponse(val ok: Boolean = false, val message: String? = null)

@Serializable
data class QrLinkCreateResponse(
    val ok: Boolean = false,
    val token: String? = null,
    val url: String? = null,
    val expires_in: Int? = null,
    val message: String? = null
)

@Serializable
data class QrLinkConsumeRequest(val token: String)

@Serializable
data class QrLinkConsumeResponse(
    val ok: Boolean = false,
    val auth_token: String? = null,
    val user: UserDto? = null,
    val message: String? = null
)
