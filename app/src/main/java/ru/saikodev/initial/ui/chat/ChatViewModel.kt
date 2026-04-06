package ru.saikodev.initial.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.saikodev.initial.domain.model.Chat
import ru.saikodev.initial.domain.model.Message
import ru.saikodev.initial.domain.repository.AuthRepository
import ru.saikodev.initial.domain.repository.ChatRepository
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val chatId: Int = savedStateHandle["chatId"] ?: 0

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _chat = MutableStateFlow<Chat?>(null)
    val chat: StateFlow<Chat?> = _chat

    private val _messageText = MutableStateFlow("")
    val messageText: StateFlow<String> = _messageText

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore

    private val _replyTo = MutableStateFlow<Message?>(null)
    val replyTo: StateFlow<Message?> = _replyTo

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending

    private var lastMessageId = 0
    private var hasMore = true
    private var pollJob: Job? = null
    private val currentUserId = authRepository.getSavedUser()?.id ?: 0

    init {
        loadInitialMessages()
        startPolling()
    }

    private fun loadInitialMessages() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = chatRepository.getMessages(chatId, init = true)
            _isLoading.value = false
            if (result.isSuccess) {
                val data = result.getOrNull()!!
                _messages.value = data.messages
                lastMessageId = data.messages.maxOfOrNull { it.id } ?: 0
                if (data.chats != null) {
                    _chat.value = data.chats.find { it.chatId == chatId }
                }
            }
        }
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                delay(3000)
                if (_messageText.value.isNotBlank()) continue // Don't poll while typing
                val result = chatRepository.getMessages(chatId, init = false, afterId = lastMessageId)
                if (result.isSuccess) {
                    val data = result.getOrNull()!!
                    // Handle deletions
                    if (data.deletedIds.isNotEmpty()) {
                        _messages.value = _messages.value.filter { it.id !in data.deletedIds }
                    }
                    // Handle new messages
                    if (data.messages.isNotEmpty()) {
                        val currentIds = _messages.value.map { it.id }.toSet()
                        val newMsgs = data.messages.filter { it.id !in currentIds }
                        if (newMsgs.isNotEmpty()) {
                            _messages.value = _messages.value + newMsgs
                        }
                        lastMessageId = data.messages.maxOfOrNull { it.id } ?: lastMessageId
                    }
                }
            }
        }
    }

    fun loadMoreMessages() {
        if (_isLoadingMore.value || !hasMore || _messages.value.isEmpty()) return
        viewModelScope.launch {
            _isLoadingMore.value = true
            val minId = _messages.value.minOfOrNull { it.id } ?: return@launch
            val result = chatRepository.loadHistory(chatId, minId)
            _isLoadingMore.value = false
            if (result.isSuccess) {
                val older = result.getOrNull()!!.messages
                if (older.isNotEmpty()) {
                    _messages.value = older + _messages.value
                }
                if (older.size < 50) hasMore = false
            }
        }
    }

    fun sendMessage() {
        val text = _messageText.value.trim()
        if (text.isEmpty()) return
        val chat = _chat.value ?: return
        val toSignalId = chat.partnerSignalId ?: return

        val replyId = _replyTo.value?.id
        _replyTo.value = null

        viewModelScope.launch {
            _isSending.value = true
            val result = chatRepository.sendMessage(toSignalId, text, replyId)
            _isSending.value = false
            if (result.isSuccess) {
                _messageText.value = ""
                // The message will come through polling
            }
        }
    }

    fun deleteMessage(messageId: Int) {
        viewModelScope.launch {
            chatRepository.deleteMessage(messageId)
            _messages.value = _messages.value.filter { it.id != messageId }
        }
    }

    fun setReplyTo(message: Message?) {
        _replyTo.value = message
    }

    fun onMessageTextChanged(text: String) {
        _messageText.value = text
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
    }
}
