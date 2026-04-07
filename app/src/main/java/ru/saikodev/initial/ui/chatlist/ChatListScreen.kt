@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class
)

package ru.saikodev.initial.ui.chatlist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PullToRefreshBox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.saikodev.initial.domain.model.Chat
import ru.saikodev.initial.domain.model.User
import ru.saikodev.initial.util.MediaUtils

/**
 * Check whether a partner is considered "online" based on lastSeen timestamp.
 * Returns true if lastSeen is within the last 5 minutes.
 */
private fun isOnline(lastSeen: Long?): Boolean {
    if (lastSeen == null) return false
    return System.currentTimeMillis() / 1000 - lastSeen < 300
}

@Composable
fun ChatListScreen(
    viewModel: ChatListViewModel,
    onChatClick: (chatId: Int, signalId: String?, partnerName: String?) -> Unit,
    onNewChat: (signalId: String, partnerName: String) -> Unit,
    onSettingsClick: () -> Unit,
    onQrScan: (loginToken: String?, linkToken: String?) -> Unit
) {
    val chats by viewModel.chats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val userSearchResults by viewModel.userSearchResults.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val pinnedChats = remember(chats) { chats.filter { it.isPinned }.sortedByDescending { it.pinOrder } }
    val regularChats = remember(chats) { chats.filter { !it.isPinned } }
    val savedMessagesChat = remember(chats) { chats.find { it.isSavedMsgs } }

    var isRefreshing by remember { mutableStateOf(false) }
    var contextMenuChat by remember { mutableStateOf<Chat?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isSearching) "" else "Чаты",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
                actions = {
                    if (isSearching) {
                        IconButton(onClick = { viewModel.clearSearch() }) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть поиск")
                        }
                    } else {
                        IconButton(onClick = { viewModel.onSearchQueryChanged(" ") }) {
                            Icon(Icons.Default.Search, contentDescription = "Поиск")
                        }
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Default.Settings, contentDescription = "Настройки")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!isSearching) {
                FloatingActionButton(
                    onClick = { /* TODO: New chat dialog */ },
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Новый чат",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Pull-to-refresh wrapper ────────────────────────────────────
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    scope.launch {
                        isRefreshing = true
                        delay(800)
                        isRefreshing = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // ── Search bar ─────────────────────────────────────────
                    if (isSearching) {
                        OutlinedTextField(
                            value = searchQuery.trim(),
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            placeholder = { Text("Поиск") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(24.dp),
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }

                    // ── Loading state ──────────────────────────────────────
                    if (isLoading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            // ── Quick access: search idle state ────────────
                            if (isSearching && searchQuery.trim().isEmpty()) {
                                savedMessagesChat?.let { savedChat ->
                                    item {
                                        Text(
                                            "Быстрый доступ",
                                            modifier = Modifier.padding(
                                                horizontal = 16.dp, vertical = 8.dp
                                            ),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    item(key = "quick_saved_${savedChat.chatId}") {
                                        ChatItemRow(
                                            chat = savedChat,
                                            currentUserId = currentUser?.id,
                                            onClick = {
                                                onChatClick(
                                                    savedChat.chatId,
                                                    savedChat.partnerSignalId,
                                                    savedChat.partnerName
                                                )
                                            }
                                        )
                                    }
                                }
                            }

                            // ── Global search results (users from API) ────
                            if (isSearching && searchQuery.trim().length >= 2) {
                                if (userSearchResults.isNotEmpty()) {
                                    item {
                                        Text(
                                            "Глобальный поиск",
                                            modifier = Modifier.padding(
                                                horizontal = 16.dp, vertical = 8.dp
                                            ),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    items(
                                        userSearchResults,
                                        key = { "user_${it.id}" }
                                    ) { user ->
                                        UserSearchItem(user = user, onClick = {
                                            onNewChat(
                                                user.signalId ?: "",
                                                user.nickname.ifBlank {
                                                    "@${user.signalId ?: ""}"
                                                }
                                            )
                                        })
                                    }
                                }
                            }

                            // ── Local chat filter results ─────────────────
                            if (isSearching && searchResults.isNotEmpty()) {
                                item {
                                    Text(
                                        "Чаты",
                                        modifier = Modifier.padding(
                                            horizontal = 16.dp, vertical = 8.dp
                                        ),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                items(searchResults, key = { it.chatId }) { chat ->
                                    ChatItemRow(
                                        chat = chat,
                                        currentUserId = currentUser?.id,
                                        onClick = {
                                            onChatClick(
                                                chat.chatId,
                                                chat.partnerSignalId,
                                                chat.partnerName
                                            )
                                        }
                                    )
                                }
                            }

                            // ── Normal chat list (not searching) ──────────
                            if (!isSearching) {
                                // Pinned chats
                                if (pinnedChats.isNotEmpty()) {
                                    item {
                                        Text(
                                            "Закреплённые",
                                            modifier = Modifier.padding(
                                                horizontal = 16.dp, vertical = 8.dp
                                            ),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    items(pinnedChats, key = { it.chatId }) { chat ->
                                        ChatItemRow(
                                            chat = chat,
                                            currentUserId = currentUser?.id,
                                            onClick = {
                                                onChatClick(
                                                    chat.chatId,
                                                    chat.partnerSignalId,
                                                    chat.partnerName
                                                )
                                            },
                                            onLongClick = { contextMenuChat = chat }
                                        )
                                    }
                                }

                                // Regular chats
                                if (regularChats.isNotEmpty()) {
                                    if (pinnedChats.isNotEmpty()) {
                                        item {
                                            Text(
                                                "Все чаты",
                                                modifier = Modifier.padding(
                                                    horizontal = 16.dp, vertical = 8.dp
                                                ),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    items(regularChats, key = { it.chatId }) { chat ->
                                        ChatItemRow(
                                            chat = chat,
                                            currentUserId = currentUser?.id,
                                            onClick = {
                                                onChatClick(
                                                    chat.chatId,
                                                    chat.partnerSignalId,
                                                    chat.partnerName
                                                )
                                            },
                                            onLongClick = { contextMenuChat = chat }
                                        )
                                    }
                                }

                                // Empty state
                                if (chats.isEmpty()) {
                                    item {
                                        Box(
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "Нет чатов",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // Bottom spacer for FAB
                            item { Spacer(modifier = Modifier.height(72.dp)) }
                        }
                    }
                }
            }

            // ── Context menu overlay ─────────────────────────────────────
            contextMenuChat?.let { chat ->
                DropdownMenu(
                    expanded = contextMenuChat != null,
                    onDismissRequest = { contextMenuChat = null }
                ) {
                    // Pin / Unpin
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.PushPin,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(if (chat.isPinned) "Открепить" else "Закрепить")
                            }
                        },
                        onClick = {
                            viewModel.pinChat(chat.chatId, !chat.isPinned)
                            contextMenuChat = null
                        }
                    )

                    // Mute / Unmute
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (chat.isMuted) Icons.Default.Notifications
                                    else Icons.Default.NotificationsOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(if (chat.isMuted) "Включить звук" else "Без звука")
                            }
                        },
                        onClick = {
                            viewModel.muteChat(chat.chatId)
                            contextMenuChat = null
                        }
                    )

                    HorizontalDivider()

                    // Delete
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "Удалить",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        onClick = {
                            viewModel.deleteChat(chat.chatId)
                            contextMenuChat = null
                            scope.launch {
                                snackbarHostState.showSnackbar("Чат удалён")
                            }
                        }
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Chat Avatar — Coil image loading with initials fallback, system icons,
// and online status indicator
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun ChatAvatar(chat: Chat) {
    val context = LocalContext.current

    Box(
        modifier = Modifier.size(56.dp),
        contentAlignment = Alignment.Center
    ) {
        // ── Background: colored circle with initials or special icon ─────
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(MediaUtils.getAvatarColor(chat.partnerName))),
            contentAlignment = Alignment.Center
        ) {
            when {
                chat.isSavedMsgs -> Icon(
                    Icons.Default.Bookmark,
                    contentDescription = "Заметки",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                chat.partnerIsSystem -> Icon(
                    Icons.Default.Security,
                    contentDescription = "Initial",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                else -> Text(
                    MediaUtils.initials(chat.partnerName),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // ── Coil image overlay (covers initials on success) ─────────────
        if (!chat.partnerAvatar.isNullOrBlank() && !chat.isSavedMsgs && !chat.partnerIsSystem) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(chat.partnerAvatar)
                    .crossfade(true)
                    .build(),
                contentDescription = chat.partnerName,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        // ── Online status dot (green, bottom-end of avatar) ─────────────
        if (isOnline(chat.partnerLastSeen) && !chat.isSavedMsgs && !chat.partnerIsSystem) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4CAF50))
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Typing indicator — animated dots cycling "печатает", "печатает.", etc.
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun TypingIndicator(modifier: Modifier = Modifier) {
    var dotCount by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            dotCount = (dotCount % 3) + 1
        }
    }

    Text(
        text = "печатает${".".repeat(dotCount)}",
        fontSize = 14.sp,
        fontStyle = FontStyle.Italic,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

// ═══════════════════════════════════════════════════════════════════════════
// Chat item row — avatar, name + verified badge, message preview, checkmarks,
// typing indicator, pin/mute/unread badges
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun ChatItemRow(
    chat: Chat,
    currentUserId: Int?,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val isOutgoing = currentUserId != null && chat.lastSenderId == currentUserId

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── Avatar with online dot ───────────────────────────────────────
        ChatAvatar(chat = chat)

        Spacer(modifier = Modifier.width(12.dp))

        // ── Content column ──────────────────────────────────────────────
        Column(modifier = Modifier.weight(1f)) {
            // Top row: partner name + verified badge + timestamp
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = when {
                            chat.isSavedMsgs -> "Заметки"
                            chat.partnerIsSystem -> "Initial"
                            chat.partnerName.isNullOrBlank() -> "@${chat.partnerSignalId ?: ""}"
                            else -> chat.partnerName
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Verified badge
                    if (chat.partnerIsVerified && !chat.isSavedMsgs && !chat.partnerIsSystem) {
                        Icon(
                            Icons.Default.Verified,
                            contentDescription = "Подтверждён",
                            modifier = Modifier
                                .size(18.dp)
                                .padding(start = 4.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Timestamp
                chat.lastTime?.let { time ->
                    Text(
                        MediaUtils.formatChatTime(time),
                        fontSize = 12.sp,
                        color = if (chat.unreadCount > 0)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Bottom row: checkmarks + message preview / typing + indicators
            Row(verticalAlignment = Alignment.CenterVertically) {
                // ── Read checkmarks for outgoing messages ─────────────────
                if (isOutgoing && !chat.lastMessage.isNullOrBlank()) {
                    Icon(
                        if (chat.isRead) Icons.Default.DoneAll else Icons.Default.Done,
                        contentDescription = if (chat.isRead) "Прочитано" else "Доставлено",
                        modifier = Modifier.size(16.dp),
                        tint = if (chat.isRead) Color(0xFF4FC3F7)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }

                // ── Last message preview or typing indicator ─────────────
                if (chat.partnerIsTyping) {
                    TypingIndicator(modifier = Modifier.weight(1f))
                } else {
                    Text(
                        text = when {
                            chat.isSavedMsgs && chat.lastMessage.isNullOrBlank() ->
                                "Сохранённые сообщения"
                            chat.lastMessage != null -> {
                                val prefix = if (isOutgoing) "Вы: " else ""
                                prefix + MediaUtils.hideSpoilerText(chat.lastMessage).take(80)
                            }
                            chat.lastMediaType == "video" -> "🎥 Видео"
                            chat.lastMediaType == "voice" -> "🎤 Голосовое сообщение"
                            chat.lastMediaType == "image" -> "🖼 Фото"
                            chat.lastMediaType == "document" -> "📄 Документ"
                            else -> ""
                        },
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // ── Status indicators ────────────────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Pin icon
                    if (chat.isPinned) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = "Закреплено",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Mute icon
                    if (chat.isMuted) {
                        Icon(
                            Icons.Default.NotificationsOff,
                            contentDescription = "Без звука",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Unread badge
                    if (chat.unreadCount > 0) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Text(
                                "${chat.unreadCount}",
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// User search result item — Coil avatar, verified badge
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun UserSearchItem(user: User, onClick: () -> Unit) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with Coil image loading
        Box(
            modifier = Modifier.size(56.dp),
            contentAlignment = Alignment.Center
        ) {
            // Background initials
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(MediaUtils.getAvatarColor(user.nickname))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    MediaUtils.initials(user.nickname),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Coil image overlay
            if (!user.avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(user.avatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = user.nickname,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    user.nickname.ifBlank { "@${user.signalId ?: ""}" },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                if (user.isVerified) {
                    Icon(
                        Icons.Default.Verified,
                        contentDescription = "Подтверждён",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (user.signalId != null) {
                Text(
                    "@${user.signalId}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
