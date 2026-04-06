package ru.saikodev.initial.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.saikodev.initial.data.preferences.AppTheme
import ru.saikodev.initial.data.preferences.SettingsManager
import ru.saikodev.initial.data.preferences.TokenManager
import ru.saikodev.initial.domain.model.User
import ru.saikodev.initial.domain.repository.AuthRepository
import ru.saikodev.initial.domain.repository.ProfileRepository
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val settingsManager: SettingsManager,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _isLoggedIn = MutableStateFlow(authRepository.isLoggedIn)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _currentUser = MutableStateFlow(authRepository.getSavedUser())
    val currentUser: StateFlow<User?> = _currentUser

    val theme: StateFlow<AppTheme> = settingsManager.theme
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppTheme.DARK)

    val notificationsEnabled = settingsManager.notificationsEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val soundEnabled = settingsManager.soundEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val anonNotifications = settingsManager.anonNotifications
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val enterSends = settingsManager.enterSends
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val fontSize = settingsManager.fontSize
        .stateIn(viewModelScope, SharingStarted.Eagerly, 15)

    init {
        refreshUser()
    }

    private fun refreshUser() {
        viewModelScope.launch {
            try {
                val result = profileRepository.getMe()
                if (result.isSuccess) {
                    _currentUser.value = result.getOrNull()
                }
            } catch (_: Exception) {}
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _isLoggedIn.value = false
            _currentUser.value = null
        }
    }

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            settingsManager.setTheme(theme)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setNotificationsEnabled(enabled)
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setSoundEnabled(enabled)
        }
    }

    fun setAnonNotifications(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setAnonNotifications(enabled)
        }
    }

    fun setEnterSends(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setEnterSends(enabled)
        }
    }

    fun setFontSize(size: Int) {
        viewModelScope.launch {
            settingsManager.setFontSize(size)
        }
    }
}
