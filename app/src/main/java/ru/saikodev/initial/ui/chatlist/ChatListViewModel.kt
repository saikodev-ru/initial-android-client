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

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _searchResults = MutableStateFlow<List<Chat>>(emptyList())
    val searchResults: StateFlow<List<Chat>> = _searchResults

    private val _userSearchResults = MutableStateFlow<List<User>>(emptyList())
    val userSearchResults: StateFlow<List<User>> = _userSearchResults

    private val _currentUser = MutableStateFlow(authRepository.getSavedUser())
    val currentUser: StateFlow<User?> = _currentUser

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    private val _selectedChat = MutableStateFlow<Chat?>(null)
    val selectedChat: StateFlow<Chat?> = _selectedChat

    private var pollJob: Job? = null

    init {
        loadChats()
        startPolling()
    }

    private fun loadChats() {
        viewModelScope.launch {
            val result = chatRepository.loadChats()
            if (result.isSuccess) {
                val chats = result.getOrNull() ?: emptyList()
                Log.d("ChatListVM", "Loaded ${chats.size} chats")
                _chats.value = chats
            } else {
                Log.e("ChatListVM", "Failed to load chats", result.exceptionOrNull())
            }
        }
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                delay(5000)
                val result = chatRepository.loadChats()
                if (result.isSuccess) {
                    val chats = result.getOrNull() ?: emptyList()
                    if (chats.isNotEmpty()) {
                        _chats.value = chats
                    }
                }
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
            val result = chatRepository.searchUsers(query)
            if (result.isSuccess) {
                _userSearchResults.value = result.getOrNull() ?: emptyList()
            }
        }
    }

    fun selectChat(chat: Chat) {
        _selectedChat.value = chat
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _isSearching.value = false
        _searchResults.value = emptyList()
        _userSearchResults.value = emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
    }
}
