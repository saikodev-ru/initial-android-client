package ru.saikodev.initial.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.hilt.navigation.compose.hiltViewModel
import ru.saikodev.initial.domain.model.Message
import ru.saikodev.initial.ui.theme.*
import ru.saikodev.initial.util.MediaUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: Int,
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val chat by viewModel.chat.collectAsState()
    val messageText by viewModel.messageText.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val replyTo by viewModel.replyTo.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    chat?.displayName ?: "",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    when {
                        chat?.partnerIsTyping == true -> "печатает…"
                        chat?.isOnline == true -> "в сети"
                        chat?.partnerLastSeen != null -> MediaUtils.formatLastSeen(chat!!.partnerLastSeen)
                        else -> "не в сети"
                    },
                    fontSize = 12.sp,
                    color = if (chat?.partnerIsTyping == true) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider()

        // Messages List
        if (isLoading) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        onLongClick = { /* Context menu */ },
                        onReply = { viewModel.setReplyTo(message) }
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }
        }

        // Reply Bar
        if (replyTo != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VerticalDivider(
                        modifier = Modifier
                            .height(32.dp)
                            .width(3.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            replyTo!!.nickname ?: "",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            replyTo!!.body?.take(60) ?: "",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { viewModel.setReplyTo(null) }) {
                        Icon(Icons.Default.Close, contentDescription = "Отмена", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // Input Bar
        HorizontalDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /* Attach file */ }) {
                Icon(Icons.Default.AttachFile, contentDescription = "Прикрепить")
            }

            OutlinedTextField(
                value = messageText,
                onValueChange = { viewModel.onMessageTextChanged(it) },
                placeholder = { Text("Сообщение…") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                maxLines = 4
            )

            IconButton(
                onClick = { viewModel.sendMessage() },
                enabled = messageText.isNotBlank()
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "Отправить",
                    tint = if (messageText.isNotBlank()) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: Message,
    onLongClick: () -> Unit,
    onReply: () -> Unit
) {
    val isMe = message.senderId == 0 // TODO: Compare with actual user ID

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        if (!isMe) {
            // Avatar for other's messages (could be hidden for consecutive messages)
            InitialAvatar(
                name = message.nickname,
                avatarUrl = message.avatarUrl,
                size = 32.dp,
                modifier = Modifier.padding(end = 6.dp)
            )
        }

        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (isMe) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .clickable { onReply() }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (!isMe && message.nickname != null) {
                Text(
                    message.nickname!!,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isMe) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

            // Reply reference
            if (message.replyTo != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isMe) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "↩ Ответ",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Message body
            if (!message.body.isNullOrBlank()) {
                Text(
                    message.body!!,
                    fontSize = 15.sp,
                    color = if (isMe) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onBackground
                )
            }

            // Media
            if (message.mediaUrl != null && message.mediaType == "image") {
                // Image placeholder - use Coil in production
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🖼", fontSize = 32.sp)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Time + status
            Row(
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (message.isEdited) {
                    Text(
                        "ред.",
                        fontSize = 11.sp,
                        color = if (isMe) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    message.sentAt?.let { MediaUtils.formatTime(it) } ?: "",
                    fontSize = 11.sp,
                    color = if (isMe) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isMe) {
                    Icon(
                        if (message.isRead) Icons.Default.DoneAll else Icons.Default.Done,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (isMe) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Reactions
            if (message.reactions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = if (isMe) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) {
                        Text(
                            message.reactions.joinToString(" ") { "${it.emoji}${it.count}" },
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
