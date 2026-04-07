package ru.saikodev.initial.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.saikodev.initial.data.api.dto.SessionDto
import ru.saikodev.initial.data.preferences.TokenManager
import ru.saikodev.initial.domain.model.User
import ru.saikodev.initial.domain.repository.AuthRepository
import ru.saikodev.initial.ui.theme.AppTheme
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _currentUser = MutableStateFlow(authRepository.getSavedUser())
    val currentUser: StateFlow<User?> = _currentUser

    private val _theme = MutableStateFlow(
        tokenManager.getSetting("app_theme")?.let { try { AppTheme.valueOf(it) } catch (_: Exception) { null } }
            ?: AppTheme.DARK
    )
    val theme: StateFlow<AppTheme> = _theme

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _sessions = MutableStateFlow<List<SessionDto>?>(null)
    val sessions: StateFlow<List<SessionDto>?> = _sessions

    init {
        refreshProfile()
        loadSessions()
    }

    fun refreshProfile() {
        viewModelScope.launch {
            try {
                val result = authRepository.getMe()
                if (result.isSuccess) _currentUser.value = result.getOrNull()
            } catch (_: Exception) {}
        }
    }

    fun setTheme(theme: AppTheme) {
        _theme.value = theme
        tokenManager.saveSetting("app_theme", theme.name)
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    fun updateProfile(nickname: String?, signalId: String?, bio: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = authRepository.updateProfile(nickname, signalId, bio)
                if (result.isSuccess) {
                    _currentUser.value = result.getOrNull()
                } else {
                    _error.value = result.exceptionOrNull()?.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun uploadAvatar(filePath: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = authRepository.uploadAvatar(filePath)
                if (result.isSuccess) {
                    refreshProfile()
                } else {
                    _error.value = result.exceptionOrNull()?.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadSessions() {
        viewModelScope.launch {
            try {
                val result = authRepository.getSessions()
                if (result.isSuccess) {
                    _sessions.value = result.getOrNull()?.sessions
                } else {
                    _error.value = result.exceptionOrNull()?.message
                }
            } catch (_: Exception) {}
        }
    }

    fun terminateSession(sessionId: String) {
        viewModelScope.launch {
            try {
                authRepository.terminateSession(sessionId)
                loadSessions()
            } catch (_: Exception) {}
        }
    }
}
