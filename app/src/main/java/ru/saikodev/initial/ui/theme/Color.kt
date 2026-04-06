package ru.saikodev.initial.ui.theme

import androidx.compose.ui.graphics.Color

// ── Brand ──
val PurpleBrand = Color(0xFF8B5CF6)
val PurpleDark = Color(0xFF7C3AED)
val PurpleLight = Color(0xFFA78BFA)

// ── Telegram-style Dark Theme ──
object DarkColors {
    val Background = Color(0xFF0E1621)       // Telegram dark bg
    val Surface = Color(0xFF17212B)          // Toolbar / cards
    val SurfaceVariant = Color(0xFF1C2733)   // Input bar, elevated
    val SurfaceElevated = Color(0xFF242F3D)  // Modals, popovers
    val BubbleIn = Color(0xFF182533)         // Incoming message bubble
    val BubbleOut = Color(0xFF2B5278)        // Outgoing message bubble
    val BubbleOutText = Color(0xFFFFFFFF)
    val BubbleInText = Color(0xFFF0F0F0)
    val Primary = Color(0xFFFFFFFF)
    val PrimaryVariant = Color(0xFF8B9DB5)   // Secondary text
    val Secondary = Color(0xFF6C7883)        // Tertiary text
    val Tertiary = Color(0xFF3E4C5A)         // Hints, disabled
    val Accent = PurpleBrand
    val Divider = Color(0xFF1F2936)
    val Online = Color(0xFF4DCD5E)           // Telegram green
    val UnreadBadge = Color(0xFF4DCD5E)      // Green unread badges
    val TypingIndicator = Color(0xFF4DCD5E)
    val LinkColor = Color(0xFF6AB2F2)        // Telegram link blue
    val Error = Color(0xFFFF5C57)
    val CodeBackground = Color(0xFF182533)
    val QuoteBar = Color(0xFF6AB2F2)
    val NavBackground = Color(0xFF17212B)
    val SearchBackground = Color(0xFF1C2733)
    val PinnedBg = Color(0xFF182533)
    val ProfileBarBg = Color(0xFF17212B)
    val ReplyBar = Color(0xFF6AB2F2)
    val DateChipBg = Color(0xFF1C2733)
    val DateChipText = Color(0xFF8B9DB5)
    val MediaOverlay = Color(0x80000000)
    val SelectionHighlight = Color(0x33FFFFFF)
}

// ── Telegram-style Light Theme ──
object LightColors {
    val Background = Color(0xFFF0F2F5)       // Telegram light bg
    val Surface = Color(0xFFFFFFFF)          // White cards
    val SurfaceVariant = Color(0xFFE4E6EB)   // Search, input
    val SurfaceElevated = Color(0xFFFFFFFF)
    val BubbleIn = Color(0xFFFFFFFF)
    val BubbleOut = Color(0xFFEFFDDE)        // Telegram green-tinted out bubble
    val BubbleOutText = Color(0xFF000000)
    val BubbleInText = Color(0xFF000000)
    val Primary = Color(0xFF000000)
    val PrimaryVariant = Color(0xFF707579)
    val Secondary = Color(0xFF8E9196)
    val Tertiary = Color(0xFFB0B3B8)
    val Accent = Color(0xFF3390EC)           // Telegram blue for light mode
    val Divider = Color(0xFFE7E8EC)
    val Online = Color(0xFF4DCD5E)
    val UnreadBadge = Color(0xFF3390EC)      // Blue unread for light mode
    val TypingIndicator = Color(0xFF3390EC)
    val LinkColor = Color(0xFF3390EC)
    val Error = Color(0xFFE53935)
    val CodeBackground = Color(0xFFF0F2F5)
    val QuoteBar = Color(0xFF3390EC)
    val NavBackground = Color(0xFFFFFFFF)
    val SearchBackground = Color(0xFFF0F2F5)
    val PinnedBg = Color(0xFFF7F7F8)
    val ProfileBarBg = Color(0xFFFFFFFF)
    val ReplyBar = Color(0xFF3390EC)
    val DateChipBg = Color(0xFFD8DED3)
    val DateChipText = Color(0xFF5E7C54)
    val MediaOverlay = Color(0x80000000)
    val SelectionHighlight = Color(0x33000000)
}

// ── AMOLED Theme ──
object AmoledColors {
    val Background = Color(0xFF000000)
    val Surface = Color(0xFF0A0A0A)
    val SurfaceVariant = Color(0xFF111111)
    val SurfaceElevated = Color(0xFF1A1A1A)
    val BubbleIn = Color(0xFF111111)
    val BubbleOut = Color(0xFF1A2B3D)        // Darker out bubble for AMOLED
    val BubbleOutText = Color(0xFFFFFFFF)
    val BubbleInText = Color(0xFFE0E0E0)
    val Primary = Color(0xFFFFFFFF)
    val PrimaryVariant = Color(0xFF8B9DB5)
    val Secondary = Color(0xFF6C7883)
    val Tertiary = Color(0xFF3E4C5A)
    val Accent = PurpleBrand
    val Divider = Color(0xFF1A1A1A)
    val Online = Color(0xFF4DCD5E)
    val UnreadBadge = Color(0xFF4DCD5E)
    val TypingIndicator = Color(0xFF4DCD5E)
    val LinkColor = Color(0xFF6AB2F2)
    val Error = Color(0xFFFF5C57)
    val CodeBackground = Color(0xFF111111)
    val QuoteBar = Color(0xFF6AB2F2)
    val NavBackground = Color(0xFF050505)
    val SearchBackground = Color(0xFF0A0A0A)
    val PinnedBg = Color(0xFF080808)
    val ProfileBarBg = Color(0xFF050505)
    val ReplyBar = Color(0xFF6AB2F2)
    val DateChipBg = Color(0xFF111111)
    val DateChipText = Color(0xFF8B9DB5)
    val MediaOverlay = Color(0x80000000)
    val SelectionHighlight = Color(0x33FFFFFF)
}
