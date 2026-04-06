package ru.saikodev.initial.ui.chat

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Reply
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.distinctUntilChanged
import ru.saikodev.initial.domain.model.Message
import ru.saikodev.initial.ui.theme.BubbleShapes
import ru.saikodev.initial.ui.theme.InitialAvatar
import ru.saikodev.initial.ui.theme.OnlineIndicator
import ru.saikodev.initial.ui.theme.ReadStatusIcon
import ru.saikodev.initial.ui.theme.TelegramColorPalette
import ru.saikodev.initial.ui.theme.TelegramColors
import ru.saikodev.initial.ui.theme.TypingIndicator
import ru.saikodev.initial.ui.theme.VerifiedBadge
import ru.saikodev.initial.ui.theme.DateSeparator
import ru.saikodev.initial.util.MediaUtils

// ─── Chat items for grouped display ───────────────────

private sealed class ChatItem {
    data class DateHeader(val dateText: String) : ChatItem()
    data class MessageItem(
        val message: Message,
        val isFirstInGroup: Boolean,
        val isLastInGroup: Boolean,
        val showAvatar: Boolean,
        val showSenderName: Boolean,
        val isOutgoing: Boolean
    ) : ChatItem()
}

// ─── Fully rounded shape for middle/first bubbles ─────

private val BubbleRounded = RoundedCornerShape(16.dp)

// ─── Build grouped chat items ─────────────────────────

private fun buildChatItems(
    messages: List<Message>,
    partnerId: Int?,
    isSavedMsgs: Boolean
): List<ChatItem> {
    if (messages.isEmpty()) return emptyList()

    val items = mutableListOf<ChatItem>()
    var lastDayStart = -1L

    for (i in messages.indices) {
        val msg = messages[i]
        val dayStart = (msg.sentAt ?: 0) / 86400

        // Insert date separator when day changes
        if (dayStart != lastDayStart) {
            items.add(ChatItem.DateHeader(MediaUtils.formatDate(msg.sentAt ?: 0)))
            lastDayStart = dayStart
        }

        val isOutgoing = isSavedMsgs || (msg.senderId != null && msg.senderId != partnerId)

        val prev = messages.getOrNull(i - 1)
        val next = messages.getOrNull(i + 1)

        val prevSameSender = prev?.senderId == msg.senderId
        val nextSameSender = next?.senderId == msg.senderId
        val prevSameDay = prev?.sentAt?.let { (it / 86400) == dayStart } ?: false
        val nextSameDay = next?.sentAt?.let { (it / 86400) == dayStart } ?: true

        val isFirstInGroup = !prevSameSender || !prevSameDay
        val isLastInGroup = !nextSameSender || !nextSameDay

        items.add(
            ChatItem.MessageItem(
                message = msg,
                isFirstInGroup = isFirstInGroup,
                isLastInGroup = isLastInGroup,
                showAvatar = !isOutgoing && isFirstInGroup,
                showSenderName = !isOutgoing && isFirstInGroup,
                isOutgoing = isOutgoing
            )
        )
    }

    return items
}

// ═══════════════════════════════════════════════════════
//  ChatScreen — Telegram-style chat
// ═══════════════════════════════════════════════════════

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
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val replyTo by viewModel.replyTo.collectAsState()
    val palette = TelegramColors.current()

    val listState = rememberLazyListState()

    // Quick lookup map for reply previews inside bubbles
    val messageMap = remember(messages) { messages.associateBy { it.id } }

    // Build grouped chat items
    val chatItems = remember(messages, chat?.partnerId, chat?.isSavedMsgs) {
        buildChatItems(messages, chat?.partnerId, chat?.isSavedMsgs)
    }

    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(chatItems.size)
        }
    }

    // Load more when scrolled near top
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index ->
                if (index <= 2) {
                    viewModel.loadMoreMessages()
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        // ─── Top bar ──────────────────────────────────
        ChatTopBar(
            chat = chat,
            palette = palette,
            onBack = onBack
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

        // ─── Messages area ────────────────────────────
        if (isLoading && messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.5.dp
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                reverseLayout = false
            ) {
                // Load-more spinner at the top
                if (isLoadingMore) {
                    item(key = "loading_more") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                items(
                    items = chatItems,
                    key = { item ->
                        when (item) {
                            is ChatItem.DateHeader -> "date_${item.dateText}"
                            is ChatItem.MessageItem -> "msg_${item.message.id}"
                        }
                    }
                ) { item ->
                    when (item) {
                        is ChatItem.DateHeader -> {
                            DateSeparator(
                                text = item.dateText,
                                modifier = Modifier.padding(vertical = 6.dp),
                                themeColors = palette
                            )
                        }

                        is ChatItem.MessageItem -> {
                            val topPadding = if (item.isFirstInGroup) 8.dp else 2.dp
                            val replyMessage = item.message.replyTo?.let { messageMap[it] }

                            MessageBubble(
                                message = item.message,
                                isOutgoing = item.isOutgoing,
                                isFirstInGroup = item.isFirstInGroup,
                                isLastInGroup = item.isLastInGroup,
                                showAvatar = item.showAvatar,
                                showSenderName = item.showSenderName,
                                replyMessage = replyMessage,
                                palette = palette,
                                modifier = Modifier.padding(top = topPadding),
                                onReply = { viewModel.setReplyTo(item.message) }
                            )
                        }
                    }
                }

                // Bottom spacing so last message isn't flush with input bar
                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }

        // ─── Reply composition bar ───────────────────
        val currentReplyTo = replyTo
        if (currentReplyTo != null) {
            ReplyComposeBar(
                replyTo = currentReplyTo,
                palette = palette,
                onClose = { viewModel.setReplyTo(null) }
            )
        }

        // ─── Input bar ────────────────────────────────
        ChatInputBar(
            messageText = messageText,
            onTextChanged = { viewModel.onMessageTextChanged(it) },
            onSend = { viewModel.sendMessage() },
            palette = palette
        )
    }
}

// ═══════════════════════════════════════════════════════
//  Top bar — custom Telegram-style header
// ═══════════════════════════════════════════════════════

@Composable
private fun ChatTopBar(
    chat: ru.saikodev.initial.domain.model.Chat?,
    palette: TelegramColorPalette,
    onBack: () -> Unit
) {
    Surface(
        color = palette.surface,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.surface)
                .padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Rounded.ArrowBack,
                    contentDescription = "Назад",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Avatar with online indicator
            Box {
                InitialAvatar(
                    name = chat?.displayName,
                    avatarUrl = chat?.partnerAvatar,
                    size = 36.dp,
                    isSavedMsgs = chat?.isSavedMsgs == true,
                    isSystem = chat?.partnerIsSystem == true
                )
                OnlineIndicator(
                    isOnline = chat?.isOnline == true,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 1.dp, end = 1.dp),
                    size = 10.dp,
                    themeColors = palette
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Name + status
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        chat?.displayName ?: "",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (chat?.partnerIsVerified == true || chat?.partnerIsTeamSignal == true) {
                        Spacer(modifier = Modifier.width(4.dp))
                        VerifiedBadge(
                            isTeamSignal = chat?.partnerIsTeamSignal == true,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Status text
                Crossfade(
                    targetState = Triple(
                        chat?.partnerIsTyping == true,
                        chat?.isOnline == true,
                        chat?.partnerLastSeen
                    ),
                    label = "status"
                ) { (isTyping, isOnline, lastSeen) ->
                    when {
                        isTyping -> Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TypingIndicator(modifier = Modifier.size(14.dp))
                            Text(
                                "печатает\u2026",
                                fontSize = 12.sp,
                                color = palette.online,
                                fontWeight = FontWeight.Normal
                            )
                        }
                        isOnline -> Text(
                            "в сети",
                            fontSize = 12.sp,
                            color = palette.online,
                            fontWeight = FontWeight.Normal
                        )
                        lastSeen != null -> Text(
                            MediaUtils.formatLastSeen(lastSeen),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Normal
                        )
                        else -> Text(
                            "не в сети",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
//  Message bubble
// ═══════════════════════════════════════════════════════

@Composable
private fun MessageBubble(
    message: Message,
    isOutgoing: Boolean,
    isFirstInGroup: Boolean,
    isLastInGroup: Boolean,
    showAvatar: Boolean,
    showSenderName: Boolean,
    replyMessage: Message?,
    palette: TelegramColorPalette,
    modifier: Modifier = Modifier,
    onReply: () -> Unit
) {
    val bubbleBg = if (isOutgoing) palette.bubbleOut else palette.bubbleIn
    val textColor = if (isOutgoing) palette.bubbleOutText else palette.bubbleInText
    val timeColor = textColor.copy(alpha = 0.55f)
    val bubbleShape: Shape = if (isLastInGroup) {
        if (isOutgoing) BubbleShapes.outgoingNoTail else BubbleShapes.incomingNoTail
    } else {
        BubbleRounded
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onReply
            ),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isOutgoing) {
            // Avatar column — 36dp avatar, always reserves space for alignment
            Box(
                modifier = Modifier.width(40.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                if (showAvatar) {
                    InitialAvatar(
                        name = message.nickname,
                        avatarUrl = message.avatarUrl,
                        size = 36.dp
                    )
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
        }

        // Bubble
        Surface(
            color = bubbleBg,
            shape = bubbleShape,
            shadowElevation = 0.dp,
            modifier = Modifier.widthIn(max = 340.dp)
        ) {
            Column(
                modifier = Modifier.padding(
                    start = 11.dp,
                    end = 11.dp,
                    top = 7.dp,
                    bottom = 6.dp
                )
            ) {
                // Sender name (incoming, first in group)
                if (showSenderName && message.nickname != null) {
                    Text(
                        text = message.nickname!!,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.linkColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (message.replyTo != null || !message.body.isNullOrBlank() || message.mediaUrl != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }

                // Reply preview inside bubble
                if (message.replyTo != null) {
                    InBubbleReplyPreview(
                        replyMessage = replyMessage,
                        isOutgoing = isOutgoing,
                        palette = palette
                    )
                    if (!message.body.isNullOrBlank() || message.mediaUrl != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                // Media content
                if (!message.mediaUrl.isNullOrBlank()) {
                    MediaContent(message = message, isOutgoing = isOutgoing)
                    if (!message.body.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                // Document attachment
                if (message.mediaType == "document" && message.mediaFileName != null) {
                    DocumentAttachment(message = message, textColor = textColor)
                    if (!message.body.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                // Message body text
                if (!message.body.isNullOrBlank()) {
                    val displayText = if (message.mediaSpoiler) {
                        MediaUtils.hideSpoilerText(message.body)
                    } else {
                        message.body
                    }
                    Text(
                        text = displayText,
                        fontSize = 15.sp,
                        color = textColor,
                        lineHeight = 20.sp
                    )
                }

                // Bottom row: time, edited, read status
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    if (message.isEdited && message.mediaUrl.isNullOrBlank()) {
                        Text(
                            "ред.",
                            fontSize = 11.sp,
                            color = timeColor
                        )
                    }
                    Text(
                        message.sentAt?.let { MediaUtils.formatTime(it) } ?: "",
                        fontSize = 11.sp,
                        color = timeColor
                    )
                    if (isOutgoing) {
                        ReadStatusIcon(
                            isRead = message.isRead,
                            modifier = Modifier.size(16.dp),
                            tint = if (message.isRead) palette.linkColor else timeColor
                        )
                    }
                }

                // Reactions
                if (message.reactions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    ReactionPills(
                        reactions = message.reactions,
                        isOutgoing = isOutgoing,
                        bubbleBg = bubbleBg,
                        textColor = textColor,
                        palette = palette
                    )
                }
            }
        }

        // Outgoing side spacer (mirror of avatar width for alignment)
        if (isOutgoing) {
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════
//  Reply preview inside message bubble
// ═══════════════════════════════════════════════════════

@Composable
private fun InBubbleReplyPreview(
    replyMessage: Message?,
    isOutgoing: Boolean,
    palette: TelegramColorPalette
) {
    val replyBg = if (isOutgoing) Color.White.copy(alpha = 0.2f)
                  else Color.Black.copy(alpha = 0.08f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(replyBg)
            .padding(start = 0.dp, top = 5.dp, bottom = 5.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left colored bar
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(palette.replyBar)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = replyMessage?.nickname ?: "Ответ",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.replyBar,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (replyMessage?.body != null) {
                Text(
                    text = replyMessage.body!!.take(50),
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
//  Media content (image / video)
// ═══════════════════════════════════════════════════════

@Composable
private fun MediaContent(
    message: Message,
    isOutgoing: Boolean
) {
    when (message.mediaType) {
        "image" -> {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Box {
                    AsyncImage(
                        model = message.mediaUrl,
                        contentDescription = message.mediaFileName ?: "Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                    // Spoiler overlay
                    if (message.mediaSpoiler) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Black.copy(alpha = 0.8f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Спойлер",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }
        "video" -> {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { /* TODO: open video player */ },
                shape = RoundedCornerShape(10.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = message.mediaUrl,
                        contentDescription = message.mediaFileName ?: "Video",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                    // Play button overlay
                    Surface(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Send,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier
                                .size(48.dp)
                                .padding(8.dp)
                        )
                    }
                    if (message.mediaSpoiler) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Black.copy(alpha = 0.8f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Спойлер",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
//  Document attachment chip
// ═══════════════════════════════════════════════════════

@Composable
private fun DocumentAttachment(
    message: Message,
    textColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(textColor.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = MediaUtils.getDocumentIcon(message.mediaFileExt ?: ""),
            fontSize = 28.sp
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = message.mediaFileName ?: "Документ",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (message.mediaFileSize != null) {
                Text(
                    text = MediaUtils.formatFileSize(message.mediaFileSize),
                    fontSize = 12.sp,
                    color = textColor.copy(alpha = 0.5f)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
//  Reaction pills
// ═══════════════════════════════════════════════════════

@Composable
private fun ReactionPills(
    reactions: List<ru.saikodev.initial.domain.model.Reaction>,
    isOutgoing: Boolean,
    bubbleBg: Color,
    textColor: Color,
    palette: TelegramColorPalette
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        reactions.forEach { reaction ->
            val pillBg = if (reaction.byMe) {
                palette.linkColor.copy(alpha = 0.2f)
            } else {
                Color.White.copy(alpha = 0.12f)
            }
            Surface(
                color = pillBg,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = reaction.emoji,
                        fontSize = 14.sp
                    )
                    if (reaction.count > 1) {
                        Text(
                            text = reaction.count.toString(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = textColor.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
//  Reply composition bar (shown above input when replying)
// ═══════════════════════════════════════════════════════

@Composable
private fun ReplyComposeBar(
    replyTo: Message,
    palette: TelegramColorPalette,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = palette.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reply icon + colored bar
            Column(
                modifier = Modifier
                    .width(36.dp)
                    .height(36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Rounded.Reply,
                    contentDescription = null,
                    tint = palette.replyBar,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Reply preview text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = replyTo.nickname ?: "Ответ",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.replyBar,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = replyTo.body?.take(60) ?: "",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Close button
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Отмена",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
//  Input bar — Telegram-style with mic/send transition
// ═══════════════════════════════════════════════════════

@Composable
private fun ChatInputBar(
    messageText: String,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    palette: TelegramColorPalette
) {
    val hasText by remember { derivedStateOf { messageText.isNotBlank() } }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = palette.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.surface)
                .padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Attachment button
            IconButton(
                onClick = { /* TODO: open attachment picker */ },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Rounded.AttachFile,
                    contentDescription = "Прикрепить",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(2.dp))

            // Text field
            OutlinedTextField(
                value = messageText,
                onValueChange = onTextChanged,
                placeholder = {
                    Text(
                        "Сообщение",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    unfocusedContainerColor = palette.background,
                    focusedContainerColor = palette.background,
                    cursorColor = palette.linkColor,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 6,
                singleLine = false
            )

            Spacer(modifier = Modifier.width(2.dp))

            // Mic / Send button with animated transition
            Crossfade(
                targetState = hasText,
                label = "send_button"
            ) { showSend ->
                if (showSend) {
                    IconButton(
                        onClick = onSend,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Send,
                            contentDescription = "Отправить",
                            tint = palette.linkColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = { /* TODO: voice recording */ },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Mic,
                            contentDescription = "Голосовое сообщение",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
