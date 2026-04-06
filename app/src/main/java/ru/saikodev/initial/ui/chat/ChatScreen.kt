package ru.saikodev.initial.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import ru.saikodev.initial.domain.model.Message
import ru.saikodev.initial.domain.model.Reaction
import ru.saikodev.initial.util.MediaUtils
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

// ── Model for flattened chat list items ──────────────────────────────────────

private sealed class ChatListItem {
    abstract val key: Any

    data class DateSeparatorItem(val timestamp: Long) : ChatListItem() {
        override val key = "date_$timestamp"
    }

    data class MessageItem(
        val message: Message,
        val isOutgoing: Boolean,
        val isGroupedWithNext: Boolean,
        val isGroupedWithPrev: Boolean
    ) : ChatListItem() {
        override val key = "msg_${message.id}"
    }
}

// ── Main ChatScreen ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    val context = LocalContext.current
    val currentUserId = viewModel.currentUserId

    // Reply state
    var replyToMessage by remember { mutableStateOf<Message?>(null) }

    // Edit state
    var editingMessage by remember { mutableStateOf<Message?>(null) }

    // Context menu state
    var contextMenuMessage by remember { mutableStateOf<Message?>(null) }

    // Populate edit text when editing
    LaunchedEffect(editingMessage) {
        if (editingMessage != null) {
            messageText = editingMessage!!.body ?: ""
        }
    }

    fun cancelReply() { replyToMessage = null }
    fun cancelEdit() {
        editingMessage = null
        messageText = ""
    }

    // Build the flattened list of ChatListItem
    val chatItems = remember(messages, currentUserId) {
        buildChatItems(messages, currentUserId)
    }

    // Auto-scroll only when user is near bottom
    val isNearBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            if (totalItems == 0) return@derivedStateOf true
            val lastVisibleIndex = listState.firstVisibleItemIndex
            val lastVisibleOffset = listState.firstVisibleItemScrollOffset
            lastVisibleIndex >= totalItems - 5 && lastVisibleOffset < 600
        }
    }

    LaunchedEffect(chatItems.size) {
        if (chatItems.isNotEmpty() && isNearBottom) {
            listState.animateScrollToItem(chatItems.size)
        }
    }

    // Dismiss errors after delay
    LaunchedEffect(error) {
        if (error != null) delay(4000)
    }

    val displayName = chatInfo?.partnerName?.ifBlank { null }
        ?: viewModel.partnerNameValue?.ifBlank { null }
        ?: "@${viewModel.signalIdValue ?: ""}"

    val isOnline = chatInfo?.let { MediaUtils.isOnline(it.partnerLastSeen) } ?: false
    val lastSeenText = chatInfo?.partnerLastSeen?.let { MediaUtils.formatLastSeen(it) }
    val isTyping = chatInfo?.partnerIsTyping == true

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AvatarImage(
                            avatarUrl = chatInfo?.partnerAvatar,
                            name = displayName,
                            size = 40.dp
                        )
                        Column {
                            Text(
                                displayName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isTyping) {
                                TypingIndicator()
                            } else {
                                Text(
                                    text = if (isOnline) "в сети"
                                    else "Был(а) ${lastSeenText ?: "давно"}",
                                    fontSize = 12.sp,
                                    color = if (isOnline) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Column {
                // Reply / Edit bar
                AnimatedVisibility(visible = replyToMessage != null || editingMessage != null) {
                    ReplyEditBar(
                        replyMessage = replyToMessage,
                        editMessage = editingMessage,
                        onCancelReply = ::cancelReply,
                        onCancelEdit = ::cancelEdit
                    )
                }

                // Error bar
                AnimatedVisibility(visible = error != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            error ?: "",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 13.sp
                        )
                    }
                }

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
                        // Attach button
                        IconButton(
                            onClick = { /* TODO: open document picker */ },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                Icons.Default.Attachment,
                                contentDescription = "Прикрепить",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            placeholder = {
                                Text(
                                    if (editingMessage != null) "Редактирование..."
                                    else "Сообщение"
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        // Send or Save edit
                        if (editingMessage != null) {
                            IconButton(
                                onClick = {
                                    if (messageText.isNotBlank()) {
                                        viewModel.editMessage(editingMessage!!.id, messageText.trim())
                                        cancelEdit()
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
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Сохранить",
                                    tint = if (messageText.isNotBlank()) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    if (messageText.isNotBlank()) {
                                        // TODO: pass replyToMessage?.id when ViewModel supports it
                                        viewModel.sendMessage(messageText)
                                        messageText = ""
                                        cancelReply()
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
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Отправить",
                                    tint = if (messageText.isNotBlank()) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading && messages.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
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

                    items(chatItems, key = { it.key }) { item ->
                        when (item) {
                            is ChatListItem.DateSeparatorItem -> {
                                DateSeparator(timestamp = item.timestamp)
                            }
                            is ChatListItem.MessageItem -> {
                                MessageBubble(
                                    message = item.message,
                                    isOutgoing = item.isOutgoing,
                                    isGroupedWithNext = item.isGroupedWithNext,
                                    isGroupedWithPrev = item.isGroupedWithPrev,
                                    onLongPress = { contextMenuMessage = item.message },
                                    onReply = { replyToMessage = it }
                                )
                            }
                        }
                    }

                    // Bottom padding
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }

        // Context menu
        contextMenuMessage?.let { msg ->
            val isOwnMessage = msg.senderId == currentUserId
            DropdownMenu(
                expanded = true,
                onDismissRequest = { contextMenuMessage = null }
            ) {
                DropdownMenuItem(
                    text = { Text("Ответить") },
                    onClick = {
                        replyToMessage = msg
                        contextMenuMessage = null
                    },
                    leadingIcon = { Icon(Icons.Default.Reply, contentDescription = null) }
                )
                if (isOwnMessage && !msg.isDeleted) {
                    DropdownMenuItem(
                        text = { Text("Редактировать") },
                        onClick = {
                            editingMessage = msg
                            messageText = msg.body ?: ""
                            contextMenuMessage = null
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                }
                if (!msg.body.isNullOrBlank() && !msg.isDeleted) {
                    DropdownMenuItem(
                        text = { Text("Копировать") },
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("message", msg.body))
                            contextMenuMessage = null
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                }
                if (isOwnMessage) {
                    DropdownMenuItem(
                        text = { Text("Удалить") },
                        onClick = {
                            viewModel.deleteMessage(msg.id)
                            contextMenuMessage = null
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }
        }
    }
}

// ── Build flattened chat items list ──────────────────────────────────────────

private fun buildChatItems(messages: List<Message>, currentUserId: Int): List<ChatListItem> {
    val items = mutableListOf<ChatListItem>()

    for (i in messages.indices) {
        val message = messages[i]
        val prevMessage = messages.getOrNull(i - 1)
        val nextMessage = messages.getOrNull(i + 1)

        // Date separator
        if (i == 0 || shouldShowDate(prevMessage?.sentAt, message.sentAt)) {
            message.sentAt?.let { items.add(ChatListItem.DateSeparatorItem(it)) }
        }

        val isOutgoing = message.senderId == currentUserId

        val isGroupedWithPrev = prevMessage?.let { prev ->
            prev.senderId == message.senderId &&
                    !prev.isDeleted && !message.isDeleted &&
                    !shouldShowDate(prev.sentAt, message.sentAt)
        } ?: false

        val isGroupedWithNext = nextMessage?.let { next ->
            next.senderId == message.senderId &&
                    !next.isDeleted && !message.isDeleted &&
                    !shouldShowDate(message.sentAt, next.sentAt)
        } ?: false

        items.add(
            ChatListItem.MessageItem(
                message = message,
                isOutgoing = isOutgoing,
                isGroupedWithNext = isGroupedWithNext,
                isGroupedWithPrev = isGroupedWithPrev
            )
        )
    }

    return items
}

// ── Typing Indicator ────────────────────────────────────────────────────────

@Composable
private fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            "печатает",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(3) { index ->
                val dotAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 600, delayMillis = index * 200),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "dot$index"
                )
                Text(
                    "•",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.alpha(dotAlpha)
                )
            }
        }
    }
}

// ── Avatar ──────────────────────────────────────────────────────────────────

@Composable
private fun AvatarImage(
    avatarUrl: String?,
    name: String,
    size: androidx.compose.ui.unit.Dp
) {
    val avatarColor = MediaUtils.getAvatarColor(name)
    val initials = MediaUtils.initials(name)
    val bgColor = Color(avatarColor)

    if (!avatarUrl.isNullOrBlank()) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = name,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                initials,
                color = Color.White,
                fontSize = (size.value * 0.38f).sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ── Message Bubble ──────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: Message,
    isOutgoing: Boolean,
    isGroupedWithNext: Boolean,
    isGroupedWithPrev: Boolean,
    onLongPress: () -> Unit,
    onReply: (Message) -> Unit
) {
    val topPadding = if (isGroupedWithPrev) 1.dp else 6.dp
    val bottomPadding = if (isGroupedWithNext) 1.dp else 6.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding, bottom = bottomPadding),
        horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
    ) {
        // ── Deleted message ──
        if (message.isDeleted) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .combinedClickable(onClick = {}, onLongClick = onLongPress)
            ) {
                Text(
                    "Сообщение удалено",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    fontStyle = FontStyle.Italic,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            return@Column
        }

        // ── Sender name + avatar (first in group, incoming only) ──
        if (!isGroupedWithPrev && !isOutgoing) {
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            ) {
                AvatarImage(
                    avatarUrl = message.avatarUrl,
                    name = message.nickname ?: "",
                    size = 32.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (message.nickname != null) {
                    Text(
                        message.nickname,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }

        // ── Reply quote ──
        message.replyBody?.let { replyBody ->
            Surface(
                modifier = Modifier.padding(
                    start = if (isOutgoing) 12.dp else if (isGroupedWithPrev) 12.dp else 52.dp,
                    end = 12.dp,
                    bottom = 2.dp
                ),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    message.replyNickname?.let {
                        Text(
                            it,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        replyBody,
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ── Main bubble ──
        Surface(
            shape = RoundedCornerShape(
                topStart = if (isGroupedWithPrev) 4.dp else 16.dp,
                topEnd = if (isGroupedWithPrev) 4.dp else 16.dp,
                bottomStart = if (isOutgoing) 16.dp else if (isGroupedWithNext) 4.dp else 16.dp,
                bottomEnd = if (isOutgoing) if (isGroupedWithNext) 4.dp else 16.dp else 16.dp
            ),
            color = if (isOutgoing) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.secondaryContainer,
            tonalElevation = if (isOutgoing) 2.dp else 0.dp,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .combinedClickable(
                    onClick = { onReply(message) },
                    onLongClick = onLongPress
                )
        ) {
            Column(
                modifier = Modifier
                    .padding(10.dp, 6.dp, 10.dp, 6.dp)
                    .widthIn(max = 320.dp)
            ) {
                // Media content
                if (message.mediaUrl != null) {
                    MediaContent(message = message, isOutgoing = isOutgoing)
                    if (message.body != null) Spacer(modifier = Modifier.height(4.dp))
                }

                // Text body
                message.body?.let { body ->
                    val displayText = if (message.mediaSpoiler) {
                        MediaUtils.hideSpoilerText(body)
                    } else {
                        body
                    }
                    val annotatedBody = buildAnnotatedMessageText(displayText, isOutgoing)
                    Text(
                        text = annotatedBody,
                        fontSize = 15.sp
                    )
                }

                // Link preview
                if (!message.body.isNullOrBlank()) {
                    val url = extractFirstUrl(message.body)
                    if (url != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        LinkPreviewCard(url = url)
                    }
                }

                // Time + edited + read checkmarks
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (message.isEdited) {
                        Text(
                            "ред.",
                            fontSize = 11.sp,
                            color = if (isOutgoing)
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                            else
                                MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                        )
                    }
                    message.sentAt?.let { time ->
                        Text(
                            MediaUtils.formatTime(time),
                            fontSize = 11.sp,
                            color = if (isOutgoing)
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            else
                                MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                    }
                    if (isOutgoing) {
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = if (message.isRead) Icons.Default.DoneAll else Icons.Default.Check,
                            contentDescription = if (message.isRead) "Прочитано" else "Отправлено",
                            modifier = Modifier.size(14.dp),
                            tint = if (isOutgoing)
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            else
                                MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // ── Reactions ──
        if (message.reactions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.padding(
                    start = if (isOutgoing) 0.dp else if (isGroupedWithPrev) 12.dp else 52.dp,
                    end = 12.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                message.reactions.forEach { reaction ->
                    ReactionChip(reaction = reaction)
                }
            }
        }
    }
}

// ── Media Content ───────────────────────────────────────────────────────────

@Composable
private fun MediaContent(message: Message, isOutgoing: Boolean) {
    val bubbleOnColor = if (isOutgoing)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSecondaryContainer

    when (message.mediaType) {
        "image" -> {
            AsyncImage(
                model = message.mediaUrl,
                contentDescription = "Фото",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 280.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        }
        "video" -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = message.mediaUrl,
                    contentDescription = "Видео",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Воспроизвести",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
        "voice" -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = "Голосовое",
                    tint = bubbleOnColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                WaveformPlaceholder(barColor = bubbleOnColor.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.width(8.dp))
                message.voiceDuration?.let { duration ->
                    Text(
                        "%d:%02d".format(duration / 60, duration % 60),
                        fontSize = 12.sp,
                        color = bubbleOnColor.copy(alpha = 0.7f)
                    )
                }
            }
        }
        "document" -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.InsertDriveFile,
                    contentDescription = "Файл",
                    tint = bubbleOnColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        message.mediaFileName ?: "Документ",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = bubbleOnColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    message.mediaFileSize?.let { size ->
                        Text(
                            MediaUtils.formatFileSize(size),
                            fontSize = 12.sp,
                            color = bubbleOnColor.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
        else -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text("📎 Медиа", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ── Waveform Placeholder ────────────────────────────────────────────────────

@Composable
private fun WaveformPlaceholder(barColor: Color) {
    Row(
        modifier = Modifier
            .height(32.dp)
            .weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(20) { index ->
            val barHeight = (8 + (index % 5) * 5).dp
            Spacer(
                modifier = Modifier
                    .width(3.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(barColor)
            )
        }
    }
}

// ── Reaction Chip ───────────────────────────────────────────────────────────

@Composable
private fun ReactionChip(reaction: Reaction) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (reaction.byMe)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
        else MaterialTheme.colorScheme.surfaceVariant,
        border = if (reaction.byMe)
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(reaction.emoji, fontSize = 13.sp)
            if (reaction.count > 1) {
                Text(
                    "${reaction.count}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Link Preview Card ───────────────────────────────────────────────────────

@Composable
private fun LinkPreviewCard(url: String) {
    val domain = try {
        android.net.Uri.parse(url).host ?: url
    } catch (_: Exception) {
        url
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { /* TODO: open URL in browser */ }
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.InsertDriveFile,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
            Text(
                domain,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ── Reply / Edit Bar ────────────────────────────────────────────────────────

@Composable
private fun ReplyEditBar(
    replyMessage: Message?,
    editMessage: Message?,
    onCancelReply: () -> Unit,
    onCancelEdit: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Accent bar
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(32.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(1.5.dp))
            )
            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (editMessage != null) {
                    Text(
                        "Редактирование",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        editMessage.body?.take(60) ?: "",
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (replyMessage != null) {
                    Text(
                        replyMessage.nickname ?: "Ответ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        replyMessage.body?.take(60) ?: "Медиа",
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = {
                onCancelReply()
                onCancelEdit()
            }) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Отмена",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Date Separator ──────────────────────────────────────────────────────────

@Composable
private fun DateSeparator(timestamp: Long?) {
    if (timestamp == null) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ) {
            Text(
                MediaUtils.formatChatTime(timestamp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Utility functions ───────────────────────────────────────────────────────

@Composable
private fun buildAnnotatedMessageText(text: String, isOutgoing: Boolean): AnnotatedString {
    val linkColor = if (isOutgoing)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.primary

    val urlPattern = Pattern.compile(
        "(?:https?://|www\\.)[\\w\\-]+(\\.[\\w\\-]+)+[\\w\\-.,@?^=%&:/~+#]*",
        Pattern.CASE_INSENSITIVE
    )
    val matcher = urlPattern.matcher(text)
    val builder = AnnotatedString.Builder()
    var lastEnd = 0

    while (matcher.find()) {
        if (matcher.start() > lastEnd) {
            builder.append(text.substring(lastEnd, matcher.start()))
        }
        builder.pushStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
        builder.append(matcher.group())
        builder.pop()
        lastEnd = matcher.end()
    }
    if (lastEnd < text.length) {
        builder.append(text.substring(lastEnd))
    }
    return builder.toAnnotatedString()
}

private fun extractFirstUrl(text: String): String? {
    val urlPattern = Pattern.compile(
        "(?:https?://|www\\.)[\\w\\-]+(\\.[\\w\\-]+)+[\\w\\-.,@?^=%&:/~+#]*",
        Pattern.CASE_INSENSITIVE
    )
    val matcher = urlPattern.matcher(text)
    return if (matcher.find()) matcher.group() else null
}

private fun shouldShowDate(prevTimestamp: Long?, currTimestamp: Long?): Boolean {
    if (prevTimestamp == null || currTimestamp == null) return prevTimestamp == null && currTimestamp != null
    val prevCal = Calendar.getInstance().apply { timeInMillis = prevTimestamp * 1000 }
    val currCal = Calendar.getInstance().apply { timeInMillis = currTimestamp * 1000 }
    return prevCal.get(Calendar.DAY_OF_YEAR) != currCal.get(Calendar.DAY_OF_YEAR) ||
            prevCal.get(Calendar.YEAR) != currCal.get(Calendar.YEAR)
}
