package ru.saikodev.initial.ui.chatlist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.saikodev.initial.domain.model.Chat
import ru.saikodev.initial.domain.model.User
import ru.saikodev.initial.ui.theme.InitialAvatar
import ru.saikodev.initial.ui.theme.OnlineIndicator
import ru.saikodev.initial.ui.theme.PinIcon
import ru.saikodev.initial.ui.theme.TypingIndicator
import ru.saikodev.initial.ui.theme.UnreadBadge
import ru.saikodev.initial.ui.theme.VerifiedBadge
import ru.saikodev.initial.util.MediaUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onChatClick: (Int) -> Unit,
    onSettingsClick: () -> Unit,
    onNewChat: () -> Unit = {},
    viewModel: ChatListViewModel = hiltViewModel()
) {
    val chats by viewModel.chats.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val userResults by viewModel.userSearchResults.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isSearching = searchQuery.isNotBlank()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ─── Header ───
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Сообщения",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onBackground
                )
                IconButton(onClick = onNewChat) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = "Новый чат",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        Icons.Rounded.Settings,
                        contentDescription = "Настройки",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // ─── Search Bar ───
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = {
                    Text(
                        "Поиск…",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearSearch() }) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Очистить",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                ),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            // ─── Content ───
            if (isSearching) {
                SearchResultsContent(
                    searchResults = searchResults,
                    userResults = userResults,
                    onChatClick = onChatClick
                )
            } else {
                ChatListContent(
                    chats = chats,
                    onChatClick = onChatClick
                )
            }

            // ─── Bottom Profile Bar ───
            if (!isSearching && currentUser != null) {
                BottomProfileBar(
                    user = currentUser!!,
                    onClick = { /* Open profile */ }
                )
            }
        }
    }
}

// ─── Chat List Content ───
@Composable
private fun ChatListContent(
    chats: List<Chat>,
    onChatClick: (Int) -> Unit
) {
    if (chats.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "💬",
                    fontSize = 48.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Нет диалогов",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Нажмите + чтобы начать",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(
                items = chats,
                key = { it.chatId }
            ) { chat ->
                ChatItem(
                    chat = chat,
                    onClick = { onChatClick(chat.chatId) }
                )
            }
        }
    }
}

// ─── Search Results Content ───
@Composable
private fun SearchResultsContent(
    searchResults: List<Chat>,
    userResults: List<User>,
    onChatClick: (Int) -> Unit
) {
    if (searchResults.isEmpty() && userResults.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Rounded.Search,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Ничего не найдено",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // Filtered chat results
            if (searchResults.isNotEmpty()) {
                items(
                    items = searchResults,
                    key = { it.chatId }
                ) { chat ->
                    ChatItem(
                        chat = chat,
                        onClick = { onChatClick(chat.chatId) }
                    )
                }
            }

            // User search results from API
            if (userResults.isNotEmpty()) {
                item {
                    Text(
                        "Пользователи",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(
                    items = userResults,
                    key = { it.id }
                ) { user ->
                    UserSearchItem(user = user)
                }
            }
        }
    }
}

// ─── Bottom Profile Bar ───
@Composable
private fun BottomProfileBar(
    user: User,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(0.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                InitialAvatar(
                    name = user.nickname,
                    avatarUrl = user.avatarUrl,
                    size = 36.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        user.nickname,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        user.signalId?.let { "@$it" } ?: user.email,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ─── Chat Item ───
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatItem(
    chat: Chat,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { /* Context menu: pin, mute, delete */ }
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ─── Avatar with online indicator ───
        Box {
            InitialAvatar(
                name = chat.partnerName,
                avatarUrl = chat.partnerAvatar,
                size = 50.dp,
                isSavedMsgs = chat.isSavedMsgs,
                isSystem = chat.partnerIsSystem
            )
            if (!chat.isSavedMsgs && !chat.partnerIsSystem) {
                OnlineIndicator(
                    isOnline = chat.isOnline,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 1.dp, end = 1.dp),
                    size = 12.dp
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // ─── Chat Info ───
        Column(modifier = Modifier.weight(1f)) {
            // Top row: Name + Timestamp
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        chat.displayName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (chat.partnerIsVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        VerifiedBadge(isTeamSignal = chat.partnerIsTeamSignal)
                    }
                }
                Text(
                    MediaUtils.formatChatTime(chat.lastTime),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Bottom row: Message preview + Pin + Unread
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (chat.partnerIsTyping) {
                    TypingIndicator(modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "печатает…",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    // Media type icon
                    val mediaIcon = getMediaTypeIcon(chat.lastMediaType)
                    if (mediaIcon != null) {
                        Text(
                            mediaIcon,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(end = 2.dp)
                        )
                    }
                    Text(
                        chat.lastMessage?.let { MediaUtils.hideSpoilerText(it) } ?: "",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (chat.isPinned) {
                    PinIcon()
                    Spacer(modifier = Modifier.width(4.dp))
                }

                if (chat.unreadCount > 0) {
                    UnreadBadge(count = chat.unreadCount)
                }
            }
        }
    }
}

// ─── User Search Item ───
@Composable
private fun UserSearchItem(user: User) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        InitialAvatar(
            name = user.nickname,
            avatarUrl = user.avatarUrl,
            size = 40.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    user.nickname,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (user.isVerified) {
                    Spacer(modifier = Modifier.width(4.dp))
                    VerifiedBadge(isTeamSignal = user.isTeamSignal)
                }
            }
            Text(
                "@${user.signalId ?: ""}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Returns an emoji icon for the last message media type.
 */
private fun getMediaTypeIcon(mediaType: String?): String? {
    return when (mediaType) {
        "photo", "image" -> "📷"
        "video" -> "🎬"
        "voice", "audio" -> "🎙"
        "document", "file" -> "📄"
        else -> null
    }
}
