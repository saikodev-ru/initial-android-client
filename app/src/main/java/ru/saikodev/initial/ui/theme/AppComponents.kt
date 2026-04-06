package ru.saikodev.initial.ui.theme

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ru.saikodev.initial.util.MediaUtils

/**
 * Initial messenger avatar component.
 * Supports: remote image, initials fallback, saved messages (bookmark), system chat (shield).
 */
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
            // Saved messages: purple tinted background with bookmark icon
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
            // System chat (Initial): purple tinted background with shield icon
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

/**
 * Online indicator dot — small green circle with white border.
 * Positioned at bottom-right of an avatar.
 */
@Composable
fun OnlineIndicator(
    isOnline: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 12.dp
) {
    if (isOnline) {
        Box(
            modifier = modifier
                .size(size)
                .background(Color.White, CircleShape)
                .padding(1.5.dp)
                .background(DarkColors.Online, CircleShape)
        )
    }
}

/**
 * Verified badge — purple checkmark in shield icon.
 * Used for verified users and team members.
 */
@Composable
fun VerifiedBadge(
    isTeamSignal: Boolean = false,
    modifier: Modifier = Modifier
) {
    Icon(
        imageVector = Icons.Rounded.Shield,
        contentDescription = if (isTeamSignal) "Команда Initial" else "Верифицирован",
        modifier = modifier.size(14.dp),
        tint = if (isTeamSignal) Color(0xFF8B5CF6) else Color(0xFF8B5CF6)
    )
}

/**
 * Unread message count badge — primary color pill.
 * Shows count (max 99+) in white text.
 */
@Composable
fun UnreadBadge(
    count: Int,
    modifier: Modifier = Modifier
) {
    if (count > 0) {
        Box(
            modifier = modifier
                .height(20.dp)
                .wrapContentWidth()
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (count > 99) "99+" else count.toString(),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Typing indicator — three animated dots with staggered alpha animation.
 * Used in chat list and chat header to show typing state.
 */
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

/**
 * Pin icon — small push pin for pinned chats.
 */
@Composable
fun PinIcon(
    modifier: Modifier = Modifier
) {
    Icon(
        imageVector = androidx.compose.material.icons.rounded.PushPin,
        contentDescription = "Закреплено",
        modifier = modifier.size(14.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
