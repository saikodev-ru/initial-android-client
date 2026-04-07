package ru.saikodev.initial.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val authRepository: AuthRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val chatId: Int = savedStateHandle["chatId"] ?: 0
    private val signalId: String? = savedStateHandle["signalId"]
    private val partnerName: String? = savedStateHandle["partnerName"]

    val currentUserId: Int get() = authRepository.getSavedUser()?.id ?: 0
    val chatIdValue: Int get() = chatId
    val signalIdValue: String? get() = signalId
    val partnerNameValue: String? get() = partnerName

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending

    private val _chatInfo = MutableStateFlow<Chat?>(null)
    val chatInfo: StateFlow<Chat?> = _chatInfo

    private val _replyTo = MutableStateFlow<Int?>(null)
    val replyTo: StateFlow<Int?> = _replyTo

    private val _editingMessageId = MutableStateFlow<Int?>(null)
    val editingMessageId: StateFlow<Int?> = _editingMessageId

    private var pollJob: Job? = null
    private var lastMessageId: Int = 0

    init {
        loadInitialMessages()
        startPolling()
    }

    private fun loadInitialMessages() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (chatId > 0) {
                    val result = chatRepository.getMessages(chatId = chatId, init = true, afterId = null)
                    if (result.isSuccess) {
                        val data = result.getOrNull()!!
                        _messages.value = data.messages
                        lastMessageId = data.messages.lastOrNull()?.id ?: 0
                        data.chats?.firstOrNull { it.chatId == chatId }?.let { _chatInfo.value = it }
                        Log.d("ChatVM", "Loaded ${data.messages.size} messages for chat $chatId")
                    } else {
                        _error.value = result.exceptionOrNull()?.message
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatVM", "loadInitialMessages failed", e)
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun startPolling() {
        if (chatId <= 0) return
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                delay(3000)
                try {
                    val result = chatRepository.getMessages(
                        chatId = chatId, init = false, afterId = lastMessageId
                    )
                    if (result.isSuccess) {
                        val data = result.getOrNull()!!
                        if (data.messages.isNotEmpty()) {
                            _messages.value = _messages.value + data.messages
                            lastMessageId = data.messages.lastOrNull()?.id ?: lastMessageId
                        }
                        // Remove deleted messages
                        if (data.deletedIds.isNotEmpty()) {
                            val deletedSet = data.deletedIds.toSet()
                            _messages.value = _messages.value.filter { it.id !in deletedSet }
                        }
                        // Update chat info
                        data.chats?.firstOrNull { it.chatId == chatId }?.let { _chatInfo.value = it }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || chatId <= 0) return
        val targetSignalId = signalId ?: return
        val replyToId = _replyTo.value
        _replyTo.value = null

        viewModelScope.launch {
            _isSending.value = true
            try {
                val result = chatRepository.sendMessage(targetSignalId, text.trim(), replyToId)
                if (result.isFailure) {
                    _error.value = result.exceptionOrNull()?.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isSending.value = false
            }
        }
    }

    fun deleteMessage(messageId: Int) {
        viewModelScope.launch {
            chatRepository.deleteMessage(messageId)
        }
    }

    fun editMessage(messageId: Int, newText: String) {
        viewModelScope.launch {
            chatRepository.editMessage(messageId, newText)
            _editingMessageId.value = null
        }
    }

    fun startEditing(messageId: Int, currentText: String) {
        _editingMessageId.value = messageId
    }

    fun setReplyTo(messageId: Int?) {
        _replyTo.value = messageId
    }

    fun copyMessageText(text: String) {
        val clipboard = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("message", text))
    }

    fun loadMoreMessages() {
        if (chatId <= 0 || _messages.value.isEmpty()) return
        viewModelScope.launch {
            val oldestId = _messages.value.firstOrNull()?.id ?: return@launch
            val result = chatRepository.loadHistory(chatId, oldestId)
            if (result.isSuccess) {
                val older = result.getOrNull()!!.messages
                _messages.value = older + _messages.value
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
    }
}
