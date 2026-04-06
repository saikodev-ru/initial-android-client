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

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

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
            } else {
                Log.e("ChatListVM", "Search failed", result.exceptionOrNull())
            }
        }
    }

    fun selectChat(chat: Chat) {
        _selectedChat.value = chat
    }

    /**
     * Open a chat with a user by their signal_id.
     *
     * Matches web client behavior (chat-list.js startChat()):
     * 1. First, check if a chat already exists in the local chat list
     * 2. If found, navigate to that chat directly
     * 3. If not found, refresh chat list from server (chat may have been
     *    created in another session) and try again
     * 4. If still not found, the ChatScreen should handle opening a new chat
     *    by sending the first message (server auto-creates the chat).
     *
     * NOTE: We do NOT send an empty message to create the chat because
     * send_message.php rejects empty messages: "Сообщение не может быть пустым"
     */
    fun openChatWithUser(signalId: String, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                // 1. Check existing chats (same as web: S.chats.find(c=>c.partner_signal_id===u.signal_id))
                val existingChat = _chats.value.find { it.partnerSignalId == signalId }
                if (existingChat != null) {
                    onResult(existingChat.chatId)
                    return@launch
                }

                // 2. Refresh chat list from server
                val refreshResult = chatRepository.loadChats()
                if (refreshResult.isSuccess) {
                    val freshChats = refreshResult.getOrNull() ?: emptyList()
                    _chats.value = freshChats
                    val freshChat = freshChats.find { it.partnerSignalId == signalId }
                    if (freshChat != null) {
                        onResult(freshChat.chatId)
                        return@launch
                    }
                }

                // 3. No chat exists yet — navigate with chat_id=0 as placeholder
                // The ChatScreen will handle creating the chat when user sends first message
                Log.d("ChatListVM", "No existing chat with @$signalId, opening as new")
                onResult(0)
            } catch (e: Exception) {
                Log.e("ChatListVM", "openChatWithUser failed", e)
                _error.value = e.message ?: "Ошибка"
            }
        }
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
