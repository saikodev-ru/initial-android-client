package ru.saikodev.initial.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.saikodev.initial.domain.model.User
import ru.saikodev.initial.domain.repository.AuthRepository
import ru.saikodev.initial.domain.repository.ProfileRepository
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _qrUrl = MutableStateFlow<String?>(null)
    val qrUrl: StateFlow<String?> = _qrUrl

    private val _qrStatus = MutableStateFlow<QrStatus>(QrStatus.Loading)
    val qrStatus: StateFlow<QrStatus> = _qrStatus

    private val _qrApprovedUser = MutableStateFlow<User?>(null)
    val qrApprovedUser: StateFlow<User?> = _qrApprovedUser

    private val _resendTimer = MutableStateFlow(0)
    val resendTimer: StateFlow<Int> = _resendTimer

    sealed class QrStatus {
        object Loading : QrStatus()
        object Ready : QrStatus()
        object Scanned : QrStatus()
        object Approved : QrStatus()
        object Expired : QrStatus()
        data class Error(val message: String) : QrStatus()
    }

    fun clearError() { _error.value = null }

    /**
     * Send verification code to email.
     * onResult is called with (email, via) on success.
     */
    fun sendCode(email: String, onResult: (String, String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = authRepository.sendCode(email)
            _isLoading.value = false
            if (result.isSuccess) {
                val via = result.getOrNull() ?: "email"
                onResult(email, via)
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Ошибка отправки кода"
            }
        }
    }

    /**
     * Verify email code.
     * onVerified is called with the authenticated User on success.
     */
    fun verifyCode(email: String, code: String, onVerified: (User) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = authRepository.verifyCode(email, code)
            _isLoading.value = false
            if (result.isSuccess) {
                val user = result.getOrNull()!!
                onVerified(user)
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Неверный код"
            }
        }
    }

    /**
     * Create user profile with nickname and optional signal_id.
     */
    fun createProfile(nickname: String, signalId: String?, onComplete: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = authRepository.createProfile(nickname, signalId)
            _isLoading.value = false
            if (result.isSuccess) {
                onComplete()
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Ошибка создания профиля"
            }
        }
    }

    fun resendCode(email: String, forceEmail: Boolean = false, onResult: (String) -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = authRepository.resendCode(email, forceEmail)
            _isLoading.value = false
            if (result.isSuccess) {
                onResult(result.getOrNull() ?: "email")
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Ошибка отправки кода"
            }
        }
    }

    // ─── QR Login ───

    private var qrToken: String? = null
    private var qrPollJob: Job? = null
    private var lastQrPollResult: ru.saikodev.initial.data.api.dto.QrPollResponse? = null

    fun startQrFlow() {
        viewModelScope.launch {
            _qrStatus.value = QrStatus.Loading
            _qrApprovedUser.value = null
            _error.value = null
            val result = authRepository.qrCreate()
            if (result.isSuccess) {
                qrToken = result.getOrNull()?.token
                _qrUrl.value = result.getOrNull()?.url
                _qrStatus.value = QrStatus.Ready
                startQrPolling()
            } else {
                _qrStatus.value = QrStatus.Error(result.exceptionOrNull()?.message ?: "Ошибка")
            }
        }
    }

    private fun startQrPolling() {
        qrPollJob?.cancel()
        qrPollJob = viewModelScope.launch {
            while (true) {
                delay(2000)
                val token = qrToken ?: break
                val result = authRepository.qrPoll(token)
                if (result.isSuccess) {
                    val pollData = result.getOrNull()
                    lastQrPollResult = pollData
                    when (pollData?.status) {
                        "scanned" -> _qrStatus.value = QrStatus.Scanned
                        "approved" -> {
                            _qrStatus.value = QrStatus.Approved
                            qrPollJob?.cancel()
                            // Save auth token FIRST
                            if (pollData.auth_token != null) {
                                authRepository.saveQrAuth(
                                    pollData.auth_token,
                                    if (pollData.user != null) {
                                        Json.encodeToString(pollData.user)
                                    } else ""
                                )
                            }
                            // Then fetch user profile
                            try {
                                val userResult = profileRepository.getMe()
                                if (userResult.isSuccess) {
                                    _qrApprovedUser.value = userResult.getOrNull()
                                }
                            } catch (_: Exception) {}
                        }
                        "expired" -> {
                            _qrStatus.value = QrStatus.Expired
                            delay(500)
                            startQrFlow()
                        }
                    }
                }
            }
        }
    }

    fun stopQrPolling() {
        qrPollJob?.cancel()
    }

    fun startResendTimer() {
        viewModelScope.launch {
            _resendTimer.value = 60
            while (_resendTimer.value > 0) {
                delay(1000)
                _resendTimer.value -= 1
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopQrPolling()
    }

    // ─── QR Scanner handling ───

    fun handleQrScan(loginToken: String, linkToken: String, onResult: (User?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (linkToken.isNotEmpty()) {
                    // Direct link - consume token
                    val result = authRepository.consumeQrLink(linkToken)
                    if (result.isSuccess) {
                        onResult(result.getOrNull())
                    } else {
                        _error.value = result.exceptionOrNull()?.message ?: "Ошибка сканирования"
                        onResult(null)
                    }
                } else if (loginToken.isNotEmpty()) {
                    // Approval flow - need to be logged in
                    val result = authRepository.approveQr(loginToken)
                    if (result.isSuccess) {
                        onResult(null) // No user change, just approved
                    } else {
                        _error.value = result.exceptionOrNull()?.message ?: "Ошибка подтверждения"
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Ошибка"
            }
            _isLoading.value = false
        }
    }
}
