package ru.saikodev.initial.ui.chatlist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.saikodev.initial.domain.model.Chat
import ru.saikodev.initial.domain.model.User
import ru.saikodev.initial.ui.theme.EmptyState
import ru.saikodev.initial.ui.theme.InitialAvatar
import ru.saikodev.initial.ui.theme.MuteIcon
import ru.saikodev.initial.ui.theme.OnlineIndicator
import ru.saikodev.initial.ui.theme.PinIcon
import ru.saikodev.initial.ui.theme.SectionLabel
import ru.saikodev.initial.ui.theme.TelegramColors
import ru.saikodev.initial.ui.theme.UnreadBadge
import ru.saikodev.initial.ui.theme.VerifiedBadge
import ru.saikodev.initial.util.MediaUtils

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
    val tgColors = TelegramColors.current()

    val pinnedChats = remember(chats) {
        chats.filter { it.isPinned }.sortedByDescending { it.pinOrder }
    }
    val regularChats = remember(chats) {
        chats.filter { !it.isPinned }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            // ─── Status bar spacer ───
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
            )

            // ─── Header ───
            ChatListHeader(
                onSettingsClick = onSettingsClick
            )

            // ─── Search Bar ───
            ChatListSearchBar(
                query = searchQuery,
                onQueryChanged = viewModel::onSearchQueryChanged,
                onClearSearch = viewModel::clearSearch
            )

            // ─── Content ───
            if (isSearching) {
                SearchResultsContent(
                    searchResults = searchResults,
                    userResults = userResults,
                    onChatClick = onChatClick,
                    tgColors = tgColors
                )
            } else {
                ChatListContent(
                    pinnedChats = pinnedChats,
                    regularChats = regularChats,
                    onChatClick = onChatClick,
                    tgColors = tgColors
                )
            }

            // ─── Bottom Profile Bar ───
            if (!isSearching && currentUser != null) {
                BottomProfileBar(
                    user = currentUser!!,
                    onClick = { /* Open profile */ }
                )
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsBottomHeight(WindowInsets.navigationBars)
                )
            }
        }

        // ─── FAB (New Chat) ───
        if (!isSearching) {
            FloatingActionButton(
                onClick = onNewChat,
                modifier = Modifier
                    .padding(end = 16.dp, bottom = if (currentUser != null) 80.dp else 16.dp)
                    .align(Alignment.BottomEnd)
                    .shadow(8.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                elevation = FloatingActionButtonDefaults.elevation(6.dp)
            ) {
                Icon(
                    Icons.Rounded.Edit,
                    contentDescription = "Новый чат",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// ─── Header ────────────────────────────────────────────────

@Composable
private fun ChatListHeader(
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Чаты",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = { /* toggle search focus or open search */ },
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                Icons.Rounded.Search,
                contentDescription = "Поиск",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                Icons.Rounded.Settings,
                contentDescription = "Настройки",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ─── Search Bar ────────────────────────────────────────────

@Composable
private fun ChatListSearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    onClearSearch: () -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        placeholder = {
            Text(
                "Поиск",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        },
        leadingIcon = {
            Icon(
                Icons.Rounded.Search,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = onClearSearch,
                    modifier = Modifier.size(32.dp)
                ) {
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
            .padding(horizontal = 16.dp, vertical = 2.dp),
        shape = RoundedCornerShape(22.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            cursorColor = MaterialTheme.colorScheme.primary
        ),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
    )
}

// ─── Chat List Content ─────────────────────────────────────

@Composable
private fun ChatListContent(
    pinnedChats: List<Chat>,
    regularChats: List<Chat>,
    onChatClick: (Int) -> Unit,
    tgColors: TelegramColorPalette
) {
    val allEmpty = pinnedChats.isEmpty() && regularChats.isEmpty()

    if (allEmpty) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 56.dp),
            contentAlignment = Alignment.Center
        ) {
            EmptyState(
                icon = "💬",
                title = "Начните общение",
                subtitle = "Напишите кому-нибудь"
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            // Pinned section
            if (pinnedChats.isNotEmpty()) {
                items(
                    items = pinnedChats,
                    key = { "pinned_${it.chatId}" }
                ) { chat ->
                    Column {
                        ChatItemRow(
                            chat = chat,
                            onClick = { onChatClick(chat.chatId) },
                            tgColors = tgColors
                        )
                        if (chat != pinnedChats.last() || regularChats.isNotEmpty()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 78.dp, end = 16.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        }
                    }
                }

                // Divider between pinned and regular chats
                if (regularChats.isNotEmpty()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 78.dp, end = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }

            // Regular chats
            items(
                items = regularChats,
                key = { "chat_${it.chatId}" }
            ) { chat ->
                Column {
                    ChatItemRow(
                        chat = chat,
                        onClick = { onChatClick(chat.chatId) },
                        tgColors = tgColors
                    )
                    if (chat != regularChats.last()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 78.dp, end = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    }
                }
            }

            // Bottom spacer for FAB + profile bar
            item {
                Spacer(modifier = Modifier.height(128.dp))
            }
        }
    }
}

// ─── Search Results Content ────────────────────────────────

@Composable
private fun SearchResultsContent(
    searchResults: List<Chat>,
    userResults: List<User>,
    onChatClick: (Int) -> Unit,
    tgColors: TelegramColorPalette
) {
    if (searchResults.isEmpty() && userResults.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            EmptyState(
                icon = "🔍",
                title = "Ничего не найдено",
                subtitle = "Попробуйте другой запрос"
            )
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // Filtered chat results
            if (searchResults.isNotEmpty()) {
                items(
                    items = searchResults,
                    key = { it.chatId }
                ) { chat ->
                    Column {
                        ChatItemRow(
                            chat = chat,
                            onClick = { onChatClick(chat.chatId) },
                            tgColors = tgColors
                        )
                        if (chat != searchResults.last() || userResults.isNotEmpty()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 78.dp, end = 16.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }

            // Global user search results
            if (userResults.isNotEmpty()) {
                item {
                    SectionLabel(text = "Глобальный поиск")
                }
                items(
                    items = userResults,
                    key = { "user_${it.id}" }
                ) { user ->
                    Column {
                        UserSearchItem(user = user)
                        if (user != userResults.last()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 78.dp, end = 16.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ─── Chat Item Row ─────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatItemRow(
    chat: Chat,
    onClick: () -> Unit,
    tgColors: TelegramColorPalette
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (chat.isPinned)
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                else Color.Transparent
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = { /* Context menu: pin, mute, delete */ }
            )
            .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ─── Avatar with online indicator ───
        Box(
            modifier = Modifier.size(52.dp)
        ) {
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
                        .padding(bottom = 0.dp, end = 0.dp),
                    size = 14.dp,
                    themeColors = tgColors
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // ─── Chat Info ───
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top row: Name + Time + Mute icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.displayName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (chat.isMuted)
                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.onBackground
                    )
                    if (chat.partnerIsVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        VerifiedBadge(isTeamSignal = chat.partnerIsTeamSignal)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (chat.isMuted) {
                        MuteIcon(modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                    }
                    Text(
                        text = MediaUtils.formatChatTime(chat.lastTime),
                        fontSize = 12.sp,
                        color = if (chat.unreadCount > 0 && !chat.isMuted)
                            tgColors.unreadBadge
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontWeight = if (chat.unreadCount > 0 && !chat.isMuted)
                            FontWeight.Medium
                        else
                            FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Bottom row: Message preview + indicators
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (chat.partnerIsTyping) {
                    Text(
                        "печатает",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = tgColors.linkColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    // Media type emoji prefix
                    val mediaIcon = getMediaTypeIcon(chat.lastMediaType)
                    Text(
                        text = if (chat.lastSenderId == null && mediaIcon != null)
                            "$mediaIcon ${chat.lastMessage?.let { MediaUtils.hideSpoilerText(it) } ?: ""}"
                        else if (chat.lastSenderId != null)
                            "Вы: ${chat.lastMessage?.let { MediaUtils.hideSpoilerText(it) } ?: ""}"
                        else if (mediaIcon != null)
                            "$mediaIcon ${chat.lastMessage?.let { MediaUtils.hideSpoilerText(it) } ?: ""}"
                        else
                            chat.lastMessage?.let { MediaUtils.hideSpoilerText(it) } ?: "",
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (chat.isMuted)
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        else if (chat.unreadCount > 0)
                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Right-side indicators: Pin + Unread badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (chat.isPinned) {
                        PinIcon()
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    if (chat.unreadCount > 0) {
                        UnreadBadge(
                            count = chat.unreadCount,
                            isMuted = chat.isMuted,
                            themeColors = tgColors
                        )
                    }
                }
            }
        }
    }
}

// ─── User Search Item ──────────────────────────────────────

@Composable
private fun UserSearchItem(user: User) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Navigate to user profile or start chat */ }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(52.dp)) {
            InitialAvatar(
                name = user.nickname,
                avatarUrl = user.avatarUrl,
                size = 50.dp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = user.nickname,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (user.isVerified) {
                    Spacer(modifier = Modifier.width(4.dp))
                    VerifiedBadge(isTeamSignal = user.isTeamSignal)
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = user.signalId?.let { "@$it" } ?: user.email,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─── Bottom Profile Bar ────────────────────────────────────

@Composable
private fun BottomProfileBar(
    user: User,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shadowElevation = 8.dp
    ) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            thickness = 0.5.dp
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(0.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(38.dp)) {
                InitialAvatar(
                    name = user.nickname,
                    avatarUrl = user.avatarUrl,
                    size = 36.dp
                )
                OnlineIndicator(
                    isOnline = true,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 0.dp, end = 0.dp),
                    size = 10.dp,
                    themeColors = TelegramColors.current()
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.nickname,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = user.signalId?.let { "@$it" } ?: user.email,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ─── Media Type Icon Helper ────────────────────────────────

/**
 * Returns an emoji icon for the last message media type.
 * Telegram uses these same-style indicators in chat previews.
 */
private fun getMediaTypeIcon(mediaType: String?): String? {
    return when (mediaType) {
        "photo", "image" -> "📷"
        "video" -> "🎬"
        "voice", "audio" -> "🎤"
        "document", "file" -> "📄"
        "sticker" -> "🎭"
        "gif", "animation" -> "GIF"
        else -> null
    }
}
