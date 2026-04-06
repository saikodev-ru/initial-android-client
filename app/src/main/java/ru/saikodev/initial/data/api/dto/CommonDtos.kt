package ru.saikodev.initial.data.api.dto
import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val ok: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val data: T? = null
)
