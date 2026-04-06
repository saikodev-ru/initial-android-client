package ru.saikodev.initial.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import ru.saikodev.initial.data.preferences.AppTheme

private val DarkColorScheme = darkColorScheme(
    primary = PurpleBrand,
    onPrimary = Color.White,
    primaryContainer = PurpleDark,
    onPrimaryContainer = Color.White,
    secondary = PurpleLight,
    background = DarkColors.Background,
    onBackground = DarkColors.Primary,
    surface = DarkColors.Surface,
    onSurface = DarkColors.Primary,
    surfaceVariant = DarkColors.SurfaceVariant,
    onSurfaceVariant = DarkColors.PrimaryVariant,
    error = DarkColors.Error,
    outline = DarkColors.Tertiary,
)

private val LightColorScheme = lightColorScheme(
    primary = PurpleBrand,
    onPrimary = Color.White,
    primaryContainer = PurpleLight,
    onPrimaryContainer = Color.White,
    secondary = PurpleLight,
    background = LightColors.Background,
    onBackground = LightColors.Primary,
    surface = LightColors.Surface,
    onSurface = LightColors.Primary,
    surfaceVariant = LightColors.SurfaceVariant,
    onSurfaceVariant = LightColors.PrimaryVariant,
    error = LightColors.Error,
    outline = LightColors.Tertiary,
)

private val AmoledColorScheme = darkColorScheme(
    primary = PurpleBrand,
    onPrimary = Color.White,
    primaryContainer = PurpleDark,
    onPrimaryContainer = Color.White,
    secondary = PurpleLight,
    background = AmoledColors.Background,
    onBackground = AmoledColors.Primary,
    surface = AmoledColors.Surface,
    onSurface = AmoledColors.Primary,
    surfaceVariant = AmoledColors.SurfaceVariant,
    onSurfaceVariant = AmoledColors.PrimaryVariant,
    error = AmoledColors.Error,
    outline = AmoledColors.Tertiary,
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
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = theme == AppTheme.LIGHT
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
