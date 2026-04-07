package ru.saikodev.initial.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.saikodev.initial.data.api.dto.QrCreateResponse
import ru.saikodev.initial.domain.model.User
import ru.saikodev.initial.domain.repository.AuthRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Stored email for code verification flow
    private val _pendingEmail = MutableStateFlow<String?>(null)
    val pendingEmail: StateFlow<String?> = _pendingEmail

    // Navigation event: code sent successfully
    private val _codeSentEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val codeSentEvent: SharedFlow<String> = _codeSentEvent

    // QR flow
    private val _qrUrl = MutableStateFlow<String?>(null)
    val qrUrl: StateFlow<String?> = _qrUrl
    private val _qrToken = MutableStateFlow<String?>(null)
    private var qrPollJob: Job? = null

    sealed class QrStatus {
        data object Loading : QrStatus()
        data class Ready(val url: String) : QrStatus()
        data object Scanned : QrStatus()
        data object Approved : QrStatus()
        data object Expired : QrStatus()
        data class Error(val message: String) : QrStatus()
    }

    private val _qrStatus = MutableStateFlow<QrStatus>(QrStatus.Loading)
    val qrStatus: StateFlow<QrStatus> = _qrStatus

    // Code verification
    private val _verifiedUser = MutableStateFlow<User?>(null)
    val verifiedUser: StateFlow<User?> = _verifiedUser

    // Resend timer
    private val _resendTimer = MutableStateFlow(0)
    val resendTimer: StateFlow<Int> = _resendTimer
    private var resendTimerJob: Job? = null

    // QR scan result
    private val _scanResult = MutableStateFlow<ScanResult?>(null)
    val scanResult: StateFlow<ScanResult?> = _scanResult

    sealed class ScanResult {
        data class LoginApproved(val user: User) : ScanResult()
        data class LinkConsumed(val user: User) : ScanResult()
        data class Error(val message: String) : ScanResult()
    }

    fun clearError() { _error.value = null }

    // ── Email Flow ──

    fun sendCode(email: String, forceEmail: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = authRepository.sendCode(email)
                if (result.isFailure) {
                    _error.value = result.exceptionOrNull()?.message ?: "Ошибка отправки кода"
                    _isLoading.value = false
                    return@launch
                }
                _pendingEmail.value = email
                _isLoading.value = false
                _codeSentEvent.emit(email)
            } catch (e: Exception) {
                _error.value = e.message ?: "Ошибка сети"
                _isLoading.value = false
            }
        }
    }

    fun verifyCode(email: String, code: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = authRepository.verifyCode(email, code)
                if (result.isSuccess) {
                    _verifiedUser.value = result.getOrNull()
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Неверный код"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Ошибка сети"
            }
            _isLoading.value = false
        }
    }

    fun createProfile(nickname: String, signalId: String?, email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = authRepository.createProfile(nickname, signalId)
                if (result.isSuccess) {
                    _verifiedUser.value = result.getOrNull()
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Ошибка создания профиля"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Ошибка сети"
            }
            _isLoading.value = false
        }
    }

    fun startResendTimer() {
        resendTimerJob?.cancel()
        _resendTimer.value = 60
        resendTimerJob = viewModelScope.launch {
            while (_resendTimer.value > 0) {
                delay(1000)
                _resendTimer.value -= 1
            }
        }
    }

    fun resendCode(email: String, forceEmail: Boolean) {
        if (_resendTimer.value > 0) return
        viewModelScope.launch {
            try {
                authRepository.resendCode(email, forceEmail)
                startResendTimer()
            } catch (_: Exception) { }
        }
    }

    // ── QR Flow ──

    fun startQrFlow() {
        qrPollJob?.cancel()
        viewModelScope.launch {
            _qrStatus.value = QrStatus.Loading
            try {
                val result = authRepository.qrCreate()
                if (result.isSuccess) {
                    val response = result.getOrNull()!!
                    _qrUrl.value = response.url
                    _qrToken.value = response.token
                    _qrStatus.value = QrStatus.Ready(response.url!!)
                    startQrPolling(response.token!!)
                } else {
                    _qrStatus.value = QrStatus.Error("Ошибка создания QR")
                }
            } catch (e: Exception) {
                _qrStatus.value = QrStatus.Error(e.message ?: "Ошибка сети")
            }
        }
    }

    private fun startQrPolling(token: String) {
        qrPollJob = viewModelScope.launch {
            while (true) {
                delay(2000)
                try {
                    val result = authRepository.qrPoll(token)
                    if (result.isSuccess) {
                        val response = result.getOrNull()!!
                        when (response.status) {
                            "scanned" -> _qrStatus.value = QrStatus.Scanned
                            "approved" -> {
                                _qrStatus.value = QrStatus.Approved
                                if (response.auth_token != null && response.user != null) {
                                    authRepository.saveQrAuth(
                                        response.auth_token,
                                        Json.encodeToString(response.user)
                                    )
                                }
                                qrPollJob?.cancel()
                            }
                            "expired" -> {
                                _qrStatus.value = QrStatus.Expired
                                startQrFlow()
                            }
                        }
                    }
                } catch (_: Exception) { }
            }
        }
    }

    fun stopQrFlow() { qrPollJob?.cancel() }

    // ── QR Scanner ──

    fun handleQrScan(data: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val loginToken = extractParam(data, "qr=")
                val linkToken = extractParam(data, "qr_link=")

                if (loginToken != null) {
                    val result = authRepository.approveQr(loginToken)
                    if (result.isSuccess) {
                        _scanResult.value = ScanResult.LoginApproved(
                            authRepository.getSavedUser() ?: User()
                        )
                    } else {
                        _scanResult.value = ScanResult.Error("Ошибка подтверждения QR")
                    }
                } else if (linkToken != null) {
                    val result = authRepository.consumeQrLink(linkToken)
                    if (result.isSuccess) {
                        _scanResult.value = ScanResult.LinkConsumed(result.getOrNull()!!)
                    } else {
                        _scanResult.value = ScanResult.Error("Ошибка QR-ссылки")
                    }
                } else if (data.length >= 32 && data.matches(Regex("[a-fA-F0-9]+"))) {
                    val result = authRepository.approveQr(data)
                    if (result.isSuccess) {
                        _scanResult.value = ScanResult.LoginApproved(
                            authRepository.getSavedUser() ?: User()
                        )
                    } else {
                        _scanResult.value = ScanResult.Error("Ошибка подтверждения QR")
                    }
                } else {
                    _scanResult.value = ScanResult.Error("Неизвестный QR-код")
                }
            } catch (e: Exception) {
                _scanResult.value = ScanResult.Error(e.message ?: "Ошибка")
            }
            _isLoading.value = false
        }
    }

    fun handleLoginToken(loginToken: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = authRepository.approveQr(loginToken)
                if (result.isSuccess) {
                    _scanResult.value = ScanResult.LoginApproved(
                        authRepository.getSavedUser() ?: User()
                    )
                } else {
                    _scanResult.value = ScanResult.Error("Ошибка подтверждения")
                }
            } catch (e: Exception) {
                _scanResult.value = ScanResult.Error(e.message ?: "Ошибка")
            }
            _isLoading.value = false
        }
    }

    fun handleLinkToken(linkToken: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = authRepository.consumeQrLink(linkToken)
                if (result.isSuccess) {
                    _scanResult.value = ScanResult.LinkConsumed(result.getOrNull()!!)
                } else {
                    _scanResult.value = ScanResult.Error("Ошибка QR-ссылки")
                }
            } catch (e: Exception) {
                _scanResult.value = ScanResult.Error(e.message ?: "Ошибка")
            }
            _isLoading.value = false
        }
    }

    private fun extractParam(url: String, param: String): String? {
        val idx = url.indexOf(param)
        if (idx == -1) return null
        val start = idx + param.length
        val end = url.indexOf('&', start).let { if (it == -1) url.length else it }
        return url.substring(start, end).takeIf { it.isNotBlank() }
    }

    override fun onCleared() {
        super.onCleared()
        qrPollJob?.cancel()
        resendTimerJob?.cancel()
    }
}
