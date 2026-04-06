package ru.saikodev.initial.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.saikodev.initial.domain.model.Message
import ru.saikodev.initial.util.MediaUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val chatInfo by viewModel.chatInfo.collectAsState()
    val error by viewModel.error.collectAsState()

    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }

    val displayName = chatInfo?.partnerName?.ifBlank { null }
        ?: viewModel.partnerNameValue?.ifBlank { null }
        ?: "@${viewModel.signalIdValue ?: ""}"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(displayName, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (chatInfo?.partnerIsTyping == true) {
                            Text("печатает...", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                }
            )
        },
        bottomBar = {
            // Message input bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 3.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                        .imePadding(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("Сообщение") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                viewModel.sendMessage(messageText)
                                messageText = ""
                            }
                        },
                        enabled = messageText.isNotBlank() && !isSending,
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (messageText.isNotBlank()) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(24.dp)
                            )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, "Отправить",
                            tint = if (messageText.isNotBlank()) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Load more button at top
                    if (messages.size >= 50) {
                        item {
                            TextButton(
                                onClick = { viewModel.loadMoreMessages() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Загрузить ранее", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    items(messages, key = { it.id }) { message ->
                        MessageBubble(message)
                    }

                    // Bottom padding for input bar
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: Message) {
    val isOutgoing = message.senderId == message.chatId // Simplified check
    // Better: compare senderId with current user's partner ID
    // For now, we'll use a simple heuristic

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.senderId == 0 || isOutgoing) Alignment.End else Alignment.Start
    ) {
        // Sender name (show for incoming messages in groups)
        if (!isOutgoing && message.nickname != null) {
            Text(
                message.nickname!!,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 12.dp, bottom = 2.dp)
            )
        }

        // Reply quote
        message.replyBody?.let { replyBody ->
            Surface(
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 2.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    message.replyNickname?.let {
                        Text(it, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    Text(replyBody, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Message bubble
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (isOutgoing) 16.dp else 4.dp,
                bottomEnd = if (isOutgoing) 4.dp else 16.dp
            ),
            color = if (isOutgoing) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp, 6.dp, 10.dp, 6.dp).widthIn(max = 320.dp)) {
                // Media placeholder
                if (message.mediaUrl != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                when (message.mediaType) {
                                    "image" -> "🖼 Фото"
                                    "video" -> "🎥 Видео"
                                    "voice" -> "🎤 Голосовое"
                                    "document" -> "📄 ${message.mediaFileName ?: "Файл"}"
                                    else -> "📎 Медиа"
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (message.body != null) Spacer(modifier = Modifier.height(4.dp))
                }

                // Text body
                message.body?.let { body ->
                    Text(
                        body,
                        fontSize = 15.sp,
                        color = if (isOutgoing) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }

                // Time + edited
                Row(
                    modifier = Modifier.align(Alignment.End),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (message.isEdited) {
                        Text("ред.", fontSize = 11.sp,
                            color = if (isOutgoing) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    message.sentAt?.let { time ->
                        Text(
                            MediaUtils.formatTime(time),
                            fontSize = 11.sp,
                            color = if (isOutgoing) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
