package ru.saikodev.initial.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ru.saikodev.initial.util.MediaUtils

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
                    .fillMaxSize()
                    .background(Color(0xFF8B5CF6).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                // Bookmark icon representation
                Text("🔖", fontSize = size.value.times(0.45).sp)
            }
        } else if (avatarUrl != null) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val bgColor = MediaUtils.getAvatarColor(name)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(bgColor)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = MediaUtils.initials(name),
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = size.value.times(0.36).sp
                )
            }
        }
    }
}

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
                .background(DarkColors.Online, CircleShape)
                .padding(2.dp)
                .background(Color.White, CircleShape)
        )
    }
}

@Composable
fun VerifiedBadge(
    isTeamSignal: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (isTeamSignal) {
        Icon(
            imageVector = Icons.Default.Verified,
            contentDescription = "Команда Initial",
            modifier = modifier.size(14.dp),
            tint = Color(0xFF8B5CF6)
        )
    } else {
        Icon(
            imageVector = Icons.Default.Verified,
            contentDescription = "Верифицирован",
            modifier = modifier.size(14.dp),
            tint = Color(0xFF8B5CF6)
        )
    }
}

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

@Composable
fun TypingIndicator(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            Surface(
                modifier = Modifier.size(5.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                shape = CircleShape
            ) {}
        }
    }
}
