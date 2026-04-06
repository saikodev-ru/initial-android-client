package ru.saikodev.initial.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.saikodev.initial.domain.model.User
import ru.saikodev.initial.domain.repository.AuthRepository
import ru.saikodev.initial.ui.theme.AppTheme
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _currentUser = MutableStateFlow(authRepository.getSavedUser())
    val currentUser: StateFlow<User?> = _currentUser

    private val _theme = MutableStateFlow(AppTheme.DARK)
    val theme: StateFlow<AppTheme> = _theme

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init { refreshProfile() }

    fun refreshProfile() {
        viewModelScope.launch {
            try {
                val result = authRepository.getMe()
                if (result.isSuccess) _currentUser.value = result.getOrNull()
            } catch (_: Exception) {}
        }
    }

    fun setTheme(theme: AppTheme) { _theme.value = theme }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}
