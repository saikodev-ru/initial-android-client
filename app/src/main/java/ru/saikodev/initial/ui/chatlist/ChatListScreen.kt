package ru.saikodev.initial.ui.chatlist

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.saikodev.initial.domain.model.Chat
import ru.saikodev.initial.domain.model.User
import ru.saikodev.initial.util.MediaUtils

@OptIn(ExperimentalMaterial3Api::class)
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
                            Icon(Icons.Default.Close, "Закрыть поиск")
                        }
                    } else {
                        IconButton(onClick = { viewModel.onSearchQueryChanged(" ") }) {
                            Icon(Icons.Default.Search, "Поиск")
                        }
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Default.Settings, "Настройки")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isSearching) {
                FloatingActionButton(
                    onClick = { /* TODO: New chat dialog */ },
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Edit, "Новый чат", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Search bar
            if (isSearching) {
                OutlinedTextField(
                    value = searchQuery.trim(),
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = { Text("Поиск") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp)) }
                )
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    // Global search results (users from API)
                    if (isSearching && searchQuery.trim().length >= 2) {
                        items(userSearchResults, key = { "user_${it.id}" }) { user ->
                            UserSearchItem(user = user, onClick = {
                                onNewChat(user.signalId ?: "", user.nickname.ifBlank { "@${user.signalId ?: ""}" })
                            })
                        }
                    }

                    // Local chat filter results
                    if (isSearching && searchResults.isNotEmpty()) {
                        item {
                            Text(
                                "Чаты",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        items(searchResults, key = { it.chatId }) { chat ->
                            ChatItemRow(
                                chat = chat,
                                onClick = { onChatClick(chat.chatId, chat.partnerSignalId, chat.partnerName) }
                            )
                        }
                    }

                    // Normal chat list (when not searching)
                    if (!isSearching) {
                        // Pinned chats
                        if (pinnedChats.isNotEmpty()) {
                            item {
                                Text(
                                    "Закреплённые",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            items(pinnedChats, key = { it.chatId }) { chat ->
                                ChatItemRow(
                                    chat = chat,
                                    onClick = { onChatClick(chat.chatId, chat.partnerSignalId, chat.partnerName) }
                                )
                            }
                        }

                        // Regular chats
                        if (regularChats.isNotEmpty()) {
                            if (pinnedChats.isNotEmpty()) {
                                item {
                                    Text(
                                        "Все чаты",
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            items(regularChats, key = { it.chatId }) { chat ->
                                ChatItemRow(
                                    chat = chat,
                                    onClick = { onChatClick(chat.chatId, chat.partnerSignalId, chat.partnerName) }
                                )
                            }
                        }

                        if (chats.isEmpty()) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text("Нет чатов", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
}

@Composable
private fun ChatItemRow(chat: Chat, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(MediaUtils.getAvatarColor(chat.partnerName))),
            contentAlignment = Alignment.Center
        ) {
            if (chat.isSavedMsgs) {
                Icon(Icons.Default.Bookmark, "Заметки", tint = Color.White, modifier = Modifier.size(28.dp))
            } else {
                Text(
                    MediaUtils.initials(chat.partnerName),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Content
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Name
                Text(
                    text = when {
                        chat.isSavedMsgs -> "Заметки"
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

                // Time
                chat.lastTime?.let { time ->
                    Text(
                        MediaUtils.formatChatTime(time),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Last message preview
                Text(
                    text = when {
                        chat.isSavedMsgs -> "Сохранённые сообщения"
                        chat.lastMessage.isNullOrBlank() -> ""
                        chat.lastSenderId == chat.partnerId -> MediaUtils.hideSpoilerText(chat.lastMessage).take(80)
                        else -> "Вы: ${MediaUtils.hideSpoilerText(chat.lastMessage).take(60)}"
                    },
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Indicators row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Pin icon
                    if (chat.isPinned) {
                        Icon(Icons.Default.PushPin, "Закреплено",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    // Mute icon
                    if (chat.isMuted) {
                        Icon(Icons.Default.NotificationsOff, "Без звука",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    // Unread badge
                    if (chat.unreadCount > 0) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Text("${chat.unreadCount}", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserSearchItem(user: User, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
                    Icon(Icons.Default.Verified, "Подтверждён", modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary)
                }
            }
            if (user.signalId != null) {
                Text("@${user.signalId}", fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
