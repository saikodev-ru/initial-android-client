package ru.saikodev.initial.ui.chatlist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.saikodev.initial.domain.model.Chat
import ru.saikodev.initial.domain.model.User
import ru.saikodev.initial.domain.repository.AuthRepository
import ru.saikodev.initial.domain.repository.ChatRepository
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    private val _searchResults = MutableStateFlow<List<Chat>>(emptyList())
    val searchResults: StateFlow<List<Chat>> = _searchResults

    private val _userSearchResults = MutableStateFlow<List<User>>(emptyList())
    val userSearchResults: StateFlow<List<User>> = _userSearchResults

    private val _currentUser = MutableStateFlow(authRepository.getSavedUser())
    val currentUser: StateFlow<User?> = _currentUser

    private var pollJob: Job? = null

    init {
        loadChats()
        startPolling()
        registerFcmToken()
    }

    private fun registerFcmToken() {
        if (!authRepository.isLoggedIn) {
            Log.d("ChatListVM", "User not logged in, skipping FCM registration")
            return
        }
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("ChatListVM", "Fetching FCM token failed", task.exception)
                    return@addOnCompleteListener
                }
                val token = task.result
                Log.d("ChatListVM", "FCM token obtained, registering with server")
                viewModelScope.launch {
                    try {
                        val result = chatRepository.registerFcmToken(token)
                        if (result.isSuccess) {
                            Log.d("ChatListVM", "FCM token registered successfully")
                        } else {
                            Log.w("ChatListVM", "FCM token registration failed: ${result.exceptionOrNull()?.message}")
                        }
                    } catch (e: Exception) {
                        Log.e("ChatListVM", "FCM registration error", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("ChatListVM", "FirebaseMessaging not available", e)
        }
    }

    private fun loadChats() {
        viewModelScope.launch {
            try {
                Log.d("ChatListVM", "Loading chats...")
                val result = chatRepository.loadChats()
                if (result.isSuccess) {
                    val chats = result.getOrNull() ?: emptyList()
                    Log.d("ChatListVM", "Loaded ${chats.size} chats")
                    _chats.value = chats
                } else {
                    Log.e("ChatListVM", "Failed to load chats", result.exceptionOrNull())
                    _error.value = result.exceptionOrNull()?.message
                }
            } catch (e: Exception) {
                Log.e("ChatListVM", "Exception loading chats", e)
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                delay(5000)
                try {
                    val result = chatRepository.loadChats()
                    if (result.isSuccess) {
                        val chats = result.getOrNull() ?: emptyList()
                        if (chats.isNotEmpty()) _chats.value = chats
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        _isSearching.value = query.isNotBlank()
        if (query.isNotBlank()) {
            filterChats(query)
            searchUsers(query)
        } else {
            _searchResults.value = emptyList()
            _userSearchResults.value = emptyList()
        }
    }

    private fun filterChats(query: String) {
        val q = query.lowercase().removePrefix("@")
        _searchResults.value = _chats.value.filter { chat ->
            (chat.partnerName ?: "").lowercase().contains(q) ||
            (chat.partnerSignalId ?: "").lowercase().contains(q)
        }
    }

    private fun searchUsers(query: String) {
        viewModelScope.launch {
            try {
                val result = chatRepository.searchUsers(query)
                if (result.isSuccess) {
                    _userSearchResults.value = result.getOrNull() ?: emptyList()
                }
            } catch (_: Exception) {}
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _isSearching.value = false
        _searchResults.value = emptyList()
        _userSearchResults.value = emptyList()
    }

    fun openChatWithUser(signalId: String, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                val existingChat = _chats.value.find { it.partnerSignalId == signalId }
                if (existingChat != null) {
                    onResult(existingChat.chatId)
                    return@launch
                }
                onResult(0) // No existing chat — open as new
            } catch (e: Exception) {
                Log.e("ChatListVM", "openChatWithUser failed", e)
            }
        }
    }

    fun muteChat(chatId: Int) {
        viewModelScope.launch {
            chatRepository.muteChat(chatId)
            loadChats()
        }
    }

    fun pinChat(chatId: Int, pin: Boolean) {
        viewModelScope.launch {
            chatRepository.pinChat(chatId, pin)
            loadChats()
        }
    }

    fun deleteChat(chatId: Int) {
        viewModelScope.launch {
            chatRepository.deleteChat(chatId)
            loadChats()
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
    }
}
