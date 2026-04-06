package ru.saikodev.initial.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import ru.saikodev.initial.data.preferences.AppTheme

private val DarkColorScheme = darkColorScheme(
    primary = PurpleBrand,
    onPrimary = Color.White,
    primaryContainer = PurpleDark,
    onPrimaryContainer = Color.White,
    secondary = PurpleLight,
    tertiary = DarkColors.LinkColor,
    background = DarkColors.Background,
    onBackground = DarkColors.Primary,
    surface = DarkColors.Surface,
    onSurface = DarkColors.Primary,
    surfaceVariant = DarkColors.SurfaceVariant,
    onSurfaceVariant = DarkColors.PrimaryVariant,
    error = DarkColors.Error,
    outline = DarkColors.Tertiary,
    outlineVariant = DarkColors.Divider,
    inverseSurface = DarkColors.SurfaceElevated,
    inverseOnSurface = DarkColors.Primary,
    inversePrimary = PurpleLight,
)

private val LightColorScheme = lightColorScheme(
    primary = LightColors.Accent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3F2FD),
    onPrimaryContainer = Color(0xFF0D47A1),
    secondary = PurpleLight,
    tertiary = LightColors.Accent,
    background = LightColors.Background,
    onBackground = LightColors.Primary,
    surface = LightColors.Surface,
    onSurface = LightColors.Primary,
    surfaceVariant = LightColors.SurfaceVariant,
    onSurfaceVariant = LightColors.PrimaryVariant,
    error = LightColors.Error,
    outline = LightColors.Tertiary,
    outlineVariant = LightColors.Divider,
    inverseSurface = LightColors.SurfaceVariant,
    inverseOnSurface = LightColors.Primary,
    inversePrimary = Color(0xFF0D47A1),
)

private val AmoledColorScheme = darkColorScheme(
    primary = PurpleBrand,
    onPrimary = Color.White,
    primaryContainer = PurpleDark,
    onPrimaryContainer = Color.White,
    secondary = PurpleLight,
    tertiary = AmoledColors.LinkColor,
    background = AmoledColors.Background,
    onBackground = AmoledColors.Primary,
    surface = AmoledColors.Surface,
    onSurface = AmoledColors.Primary,
    surfaceVariant = AmoledColors.SurfaceVariant,
    onSurfaceVariant = AmoledColors.PrimaryVariant,
    error = AmoledColors.Error,
    outline = AmoledColors.Tertiary,
    outlineVariant = AmoledColors.Divider,
    inverseSurface = AmoledColors.SurfaceElevated,
    inverseOnSurface = AmoledColors.Primary,
    inversePrimary = PurpleLight,
)

@Composable
fun InitialTheme(
    theme: AppTheme = AppTheme.DARK,
    content: @Composable () -> Unit
) {
    val colorScheme = when (theme) {
        AppTheme.LIGHT -> LightColorScheme
        AppTheme.AMOLED -> AmoledColorScheme
        AppTheme.DARK -> DarkColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = theme == AppTheme.LIGHT
            insetsController.isAppearanceLightNavigationBars = theme == AppTheme.LIGHT
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * Helper composable to get the current telegram-style color palette.
 */
object TelegramColors {
    @Composable
    fun current(): TelegramColorPalette {
        val bg = MaterialTheme.colorScheme.background
        return when {
            bg == AmoledColors.Background -> TelegramColorPalette(
                bubbleOut = AmoledColors.BubbleOut,
                bubbleIn = AmoledColors.BubbleIn,
                bubbleOutText = AmoledColors.BubbleOutText,
                bubbleInText = AmoledColors.BubbleInText,
                online = AmoledColors.Online,
                unreadBadge = AmoledColors.UnreadBadge,
                linkColor = AmoledColors.LinkColor,
                replyBar = AmoledColors.ReplyBar,
                dateChipBg = AmoledColors.DateChipBg,
                dateChipText = AmoledColors.DateChipText,
                codeBg = AmoledColors.CodeBackground,
                surface = AmoledColors.Surface,
                background = AmoledColors.Background,
            )
            // Detect light by checking if background is light
            MaterialTheme.colorScheme.background == LightColors.Background -> TelegramColorPalette(
                bubbleOut = LightColors.BubbleOut,
                bubbleIn = LightColors.BubbleIn,
                bubbleOutText = LightColors.BubbleOutText,
                bubbleInText = LightColors.BubbleInText,
                online = LightColors.Online,
                unreadBadge = LightColors.UnreadBadge,
                linkColor = LightColors.LinkColor,
                replyBar = LightColors.ReplyBar,
                dateChipBg = LightColors.DateChipBg,
                dateChipText = LightColors.DateChipText,
                codeBg = LightColors.CodeBackground,
                surface = LightColors.Surface,
                background = LightColors.Background,
            )
            else -> TelegramColorPalette(
                bubbleOut = DarkColors.BubbleOut,
                bubbleIn = DarkColors.BubbleIn,
                bubbleOutText = DarkColors.BubbleOutText,
                bubbleInText = DarkColors.BubbleInText,
                online = DarkColors.Online,
                unreadBadge = DarkColors.UnreadBadge,
                linkColor = DarkColors.LinkColor,
                replyBar = DarkColors.ReplyBar,
                dateChipBg = DarkColors.DateChipBg,
                dateChipText = DarkColors.DateChipText,
                codeBg = DarkColors.CodeBackground,
                surface = DarkColors.Surface,
                background = DarkColors.Background,
            )
        }
    }
}

data class TelegramColorPalette(
    val bubbleOut: Color,
    val bubbleIn: Color,
    val bubbleOutText: Color,
    val bubbleInText: Color,
    val online: Color,
    val unreadBadge: Color,
    val linkColor: Color,
    val replyBar: Color,
    val dateChipBg: Color,
    val dateChipText: Color,
    val codeBg: Color,
    val surface: Color,
    val background: Color,
)
