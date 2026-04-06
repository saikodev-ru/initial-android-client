package ru.saikodev.initial.ui.theme

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ru.saikodev.initial.util.MediaUtils

// ─── Avatar ───────────────────────────────────────────

@Composable
fun InitialAvatar(
    name: String?,
    avatarUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    isSavedMsgs: Boolean = false,
    isSystem: Boolean = false
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (isSavedMsgs) {
            Box(
                modifier = Modifier
                    .size(size)
                    .background(PurpleBrand.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bookmark,
                    contentDescription = "Заметки",
                    modifier = Modifier.size(size.times(0.45f)),
                    tint = PurpleBrand
                )
            }
        } else if (isSystem) {
            Box(
                modifier = Modifier
                    .size(size)
                    .background(PurpleBrand.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shield,
                    contentDescription = "Initial",
                    modifier = Modifier.size(size.times(0.45f)),
                    tint = PurpleBrand
                )
            }
        } else if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = name ?: "",
                modifier = Modifier.size(size)
            )
        } else {
            val bgColor = MediaUtils.getAvatarColor(name)
            Box(
                modifier = Modifier
                    .size(size)
                    .background(Color(bgColor), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = MediaUtils.initials(name),
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = (size.value * 0.36f).sp
                )
            }
        }
    }
}

// ─── Online Indicator ────────────────────────────────

@Composable
fun OnlineIndicator(
    isOnline: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 12.dp,
    themeColors: TelegramColorPalette = TelegramColors.current()
) {
    if (isOnline) {
        Box(
            modifier = modifier
                .size(size)
                .background(Color.White, CircleShape)
                .padding(1.5.dp)
                .background(themeColors.online, CircleShape)
        )
    }
}

// ─── Verified Badge ──────────────────────────────────

@Composable
fun VerifiedBadge(
    isTeamSignal: Boolean = false,
    modifier: Modifier = Modifier
) {
    Icon(
        imageVector = Icons.Rounded.Shield,
        contentDescription = if (isTeamSignal) "Команда Initial" else "Верифицирован",
        modifier = modifier.size(14.dp),
        tint = PurpleBrand
    )
}

// ─── Unread Badge ────────────────────────────────────

@Composable
fun UnreadBadge(
    count: Int,
    modifier: Modifier = Modifier,
    isMuted: Boolean = false,
    themeColors: TelegramColorPalette = TelegramColors.current()
) {
    if (count > 0) {
        val bgColor = if (isMuted) {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        } else {
            themeColors.unreadBadge
        }
        Box(
            modifier = modifier
                .height(20.dp)
                .background(bgColor, RoundedCornerShape(10.dp))
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (count > 99) "99+" else count.toString(),
                color = if (isMuted) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ─── Typing Indicator ────────────────────────────────

@Composable
fun TypingIndicator(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 600,
                        easing = EaseInOut,
                        delayMillis = index * 200
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "typing_dot_$index"
            )
            Surface(
                modifier = Modifier.size(5.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                shape = CircleShape
            ) {}
        }
    }
}

// ─── Pin Icon ────────────────────────────────────────

@Composable
fun PinIcon(
    modifier: Modifier = Modifier
) {
    Icon(
        imageVector = Icons.Rounded.PushPin,
        contentDescription = "Закреплено",
        modifier = modifier.size(14.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

// ─── Mute Icon ───────────────────────────────────────

@Composable
fun MuteIcon(
    modifier: Modifier = Modifier
) {
    Icon(
        imageVector = Icons.Rounded.NotificationsOff,
        contentDescription = "Без звука",
        modifier = modifier.size(14.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

// ─── Date Separator ──────────────────────────────────

@Composable
fun DateSeparator(
    text: String,
    modifier: Modifier = Modifier,
    themeColors: TelegramColorPalette = TelegramColors.current()
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = themeColors.dateChipBg,
            shape = RoundedCornerShape(16.dp),
            contentColor = themeColors.dateChipText
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─── Message Bubble Shape (with optional tail) ───────

object BubbleShapes {
    val outgoingNoTail = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = 16.dp,
        bottomEnd = 4.dp
    )

    val incomingNoTail = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = 4.dp,
        bottomEnd = 16.dp
    )
}

// ─── Checkmarks (read status) ────────────────────────

@Composable
fun ReadStatusIcon(
    isRead: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    if (isRead) {
        Icon(
            imageVector = Icons.Rounded.DoneAll,
            contentDescription = "Прочитано",
            modifier = modifier.size(16.dp),
            tint = tint
        )
    } else {
        Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = "Доставлено",
            modifier = modifier.size(16.dp),
            tint = tint
        )
    }
}

// ─── Telegram-style section label ────────────────────

@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
        modifier = modifier.padding(horizontal = 24.dp, vertical = 12.dp)
    )
}

// ─── Telegram-style clickable row ────────────────────

@Composable
fun TelegramRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading?.invoke()
        if (leading != null) Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) { content() }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

// ─── Empty State ─────────────────────────────────────

@Composable
fun EmptyState(
    icon: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 48.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                subtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
