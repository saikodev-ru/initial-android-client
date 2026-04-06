package ru.saikodev.initial.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.saikodev.initial.data.preferences.AppTheme
import ru.saikodev.initial.ui.theme.InitialAvatar
import ru.saikodev.initial.ui.theme.SectionLabel
import ru.saikodev.initial.ui.theme.TelegramRow
import ru.saikodev.initial.ui.theme.VerifiedBadge

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val theme by viewModel.theme.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val anonNotifications by viewModel.anonNotifications.collectAsState()
    val enterSends by viewModel.enterSends.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    val background = MaterialTheme.colorScheme.background

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        // ── Top bar ──────────────────────────────────────
        Surface(
            color = surface,
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Назад",
                        tint = onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    "Настройки",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ── Scrollable content ───────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {

            // ── Profile card ──────────────────────────────
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { /* Navigate to profile edit */ },
                color = surface,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    InitialAvatar(
                        name = currentUser?.nickname,
                        avatarUrl = currentUser?.avatarUrl,
                        size = 80.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            currentUser?.nickname ?: "",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = onSurface
                        )
                        if (currentUser?.isVerified == true) {
                            Spacer(modifier = Modifier.width(4.dp))
                            VerifiedBadge(
                                isTeamSignal = currentUser?.isTeamSignal == true
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        currentUser?.signalId?.let { "@$it" }
                            ?: currentUser?.email ?: "",
                        fontSize = 14.sp,
                        color = onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Изменить профиль",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Аккаунт ──────────────────────────────────
            SectionLabel(text = "Аккаунт")
            SettingsSection {
                SettingsRow(
                    icon = Icons.Default.Person,
                    iconBackgroundColor = Color(0xFFF59E0B),
                    title = "Изменить профиль",
                    onClick = { /* Navigate */ }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Уведомления ───────────────────────────────
            SectionLabel(text = "Уведомления")
            SettingsSection {
                SettingsToggleRow(
                    title = "Уведомления",
                    checked = notificationsEnabled,
                    onCheckedChange = { viewModel.setNotificationsEnabled(it) },
                    leadingIcon = Icons.Default.Notifications,
                    iconBackgroundColor = Color(0xFFF59E0B)
                )
                SettingsDivider()
                SettingsToggleRow(
                    title = "Звук",
                    checked = soundEnabled,
                    onCheckedChange = { viewModel.setSoundEnabled(it) },
                    leadingIcon = Icons.AutoMirrored.Filled.VolumeUp,
                    iconBackgroundColor = Color(0xFF4DCD5E)
                )
                SettingsDivider()
                SettingsToggleRow(
                    title = "Анонимные уведомления",
                    checked = anonNotifications,
                    onCheckedChange = { viewModel.setAnonNotifications(it) },
                    leadingIcon = Icons.Default.VisibilityOff,
                    iconBackgroundColor = Color(0xFF6AB2F2)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Чат ──────────────────────────────────────
            SectionLabel(text = "Чат")
            SettingsSection {
                SettingsToggleRow(
                    title = "Enter отправляет",
                    checked = enterSends,
                    onCheckedChange = { viewModel.setEnterSends(it) },
                    leadingIcon = Icons.Default.Person,
                    iconBackgroundColor = Color(0xFF8B5CF6)
                )
                SettingsDivider()
                TelegramRow(
                    onClick = { /* Font size picker */ },
                    leading = {
                        LeadingIconCircle(
                            icon = Icons.Default.Info,
                            iconBackgroundColor = Color(0xFFFF6B00)
                        )
                    },
                    content = {
                        Text(
                            "Размер шрифта",
                            fontSize = 15.sp,
                            color = onSurface
                        )
                    },
                    trailing = {
                        Text(
                            "$fontSize",
                            fontSize = 14.sp,
                            color = onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Оформление ────────────────────────────────
            SectionLabel(text = "Оформление")
            SettingsSection {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Тема",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ThemeCard(
                            title = "Тёмная",
                            isSelected = theme == AppTheme.DARK,
                            previewColor = Color(0xFF1A1A1A),
                            selectedBorderColor = primary,
                            onClick = { viewModel.setTheme(AppTheme.DARK) },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        ThemeCard(
                            title = "Светлая",
                            isSelected = theme == AppTheme.LIGHT,
                            previewColor = Color(0xFFF5F5F5),
                            selectedBorderColor = primary,
                            onClick = { viewModel.setTheme(AppTheme.LIGHT) },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        ThemeCard(
                            title = "AMOLED",
                            isSelected = theme == AppTheme.AMOLED,
                            previewColor = Color.Black,
                            selectedBorderColor = primary,
                            onClick = { viewModel.setTheme(AppTheme.AMOLED) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Информация ────────────────────────────────
            SectionLabel(text = "Информация")
            SettingsSection {
                SettingsRow(
                    icon = Icons.Default.Info,
                    iconBackgroundColor = Color(0xFF8E8E93),
                    title = "О приложении",
                    onClick = { /* Show about */ }
                )
                SettingsDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Leading spacer to align with rows that have icons
                    Spacer(modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        "Версия",
                        fontSize = 15.sp,
                        color = onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "1.0.0",
                        fontSize = 14.sp,
                        color = onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Выйти ─────────────────────────────────────
            TextButton(
                onClick = { viewModel.logout() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp)
            ) {
                Icon(
                    Icons.Default.Logout,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Выйти",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ─── Section card ──────────────────────────────────────

@Composable
private fun SettingsSection(
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(content = content)
    }
}

// ─── Row with leading icon circle + title + chevron ────

@Composable
private fun SettingsRow(
    icon: ImageVector,
    iconBackgroundColor: Color,
    title: String,
    onClick: () -> Unit
) {
    TelegramRow(
        onClick = onClick,
        leading = {
            LeadingIconCircle(
                icon = icon,
                iconBackgroundColor = iconBackgroundColor
            )
        },
        content = {
            Text(
                title,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        trailing = {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    )
}

// ─── Toggle row with leading icon circle ───────────────

@Composable
private fun SettingsToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    leadingIcon: ImageVector,
    iconBackgroundColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LeadingIconCircle(
            icon = leadingIcon,
            iconBackgroundColor = iconBackgroundColor
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            title,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

// ─── Leading icon circle (32 dp) ──────────────────────

@Composable
private fun LeadingIconCircle(
    icon: ImageVector,
    iconBackgroundColor: Color
) {
    Surface(
        modifier = Modifier.size(32.dp),
        color = iconBackgroundColor,
        shape = CircleShape
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .padding(8.dp)
                .size(16.dp)
        )
    }
}

// ─── Divider between section items ─────────────────────

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

// ─── Theme selection card ──────────────────────────────

@Composable
private fun ThemeCard(
    title: String,
    isSelected: Boolean,
    previewColor: Color,
    selectedBorderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .then(
                if (isSelected) Modifier.border(
                    width = 2.dp,
                    color = selectedBorderColor,
                    shape = shape
                ) else Modifier
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Mini preview rectangle
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            color = previewColor,
            shape = RoundedCornerShape(8.dp)
        ) {
            // Tiny fake chat preview
            Row(
                modifier = Modifier.padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape),
                    color = Color(0xFF8B5CF6)
                ) {}
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Surface(
                        modifier = Modifier
                            .width(28.dp)
                            .height(4.dp),
                        color = Color.Gray.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(2.dp)
                    ) {}
                    Spacer(modifier = Modifier.height(3.dp))
                    Surface(
                        modifier = Modifier
                            .width(20.dp)
                            .height(4.dp),
                        color = Color.Gray.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(2.dp)
                    ) {}
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            title,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) selectedBorderColor
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
