package ru.saikodev.initial.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.saikodev.initial.domain.repository.AuthRepository
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _qrUrl = MutableStateFlow<String?>(null)
    val qrUrl: StateFlow<String?> = _qrUrl

    private val _qrStatus = MutableStateFlow<QrStatus>(QrStatus.Loading)
    val qrStatus: StateFlow<QrStatus> = _qrStatus

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

    fun sendCode(email: String, onSuccess: (String, String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = authRepository.sendCode(email)
            _isLoading.value = false
            if (result.isSuccess) {
                // Default via, server can override
                onSuccess(email, "email")
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Ошибка отправки кода"
            }
        }
    }

    fun verifyCode(email: String, code: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = authRepository.verifyCode(email, code)
            _isLoading.value = false
            if (result.isSuccess) {
                onSuccess()
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Неверный код"
            }
        }
    }

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

    // QR Login
    private var qrToken: String? = null
    private var qrPollJob: kotlinx.coroutines.Job? = null

    fun startQrFlow() {
        viewModelScope.launch {
            _qrStatus.value = QrStatus.Loading
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
                    when (result.getOrNull()?.status) {
                        "scanned" -> _qrStatus.value = QrStatus.Scanned
                        "approved" -> {
                            _qrStatus.value = QrStatus.Approved
                            delay(1000)
                            qrPollJob?.cancel()
                            // Auth token is saved in repository
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
}
