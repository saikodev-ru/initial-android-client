package ru.saikodev.initial.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.saikodev.initial.data.preferences.AppTheme
import ru.saikodev.initial.ui.theme.InitialAvatar

@OptIn(ExperimentalMaterial3Api::class)
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
    val currentUser by viewModel.currentUser.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
            }
            Text(
                "Настройки",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Profile card
            if (currentUser != null) {
                SettingsSection {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { /* Navigate to profile edit */ }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InitialAvatar(
                            name = currentUser!!.nickname,
                            avatarUrl = currentUser!!.avatarUrl,
                            size = 56.dp
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                currentUser!!.nickname,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                currentUser!!.signalId?.let { "@$it" } ?: currentUser!!.email,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Account
            SettingsSectionTitle("Аккаунт")
            SettingsSection {
                SettingsRow(
                    icon = Icons.Default.Person,
                    iconColor = Color(0xFFF59E0B),
                    title = "Изменить профиль",
                    onClick = { /* Navigate */ }
                )
            }

            // Notifications
            SettingsSectionTitle("Уведомления")
            SettingsSection {
                SettingsToggleRow(
                    title = "Push-уведомления",
                    subtitle = "Уведомления в системе",
                    checked = notificationsEnabled,
                    onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                )
                SettingsDivider()
                SettingsToggleRow(
                    title = "Звук",
                    subtitle = "Звуковой сигнал",
                    checked = soundEnabled,
                    onCheckedChange = { viewModel.setSoundEnabled(it) }
                )
                SettingsDivider()
                SettingsToggleRow(
                    title = "Анонимно",
                    subtitle = "Скрыть имя в уведомлении",
                    checked = anonNotifications,
                    onCheckedChange = { viewModel.setAnonNotifications(it) }
                )
            }

            // Appearance
            SettingsSectionTitle("Оформление")
            SettingsSection {
                // Theme selector
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Тема",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeCard(
                            title = "Тёмная",
                            isSelected = theme == AppTheme.DARK,
                            previewColor = Color(0xFF1A1A1A),
                            onClick = { viewModel.setTheme(AppTheme.DARK) }
                        )
                        ThemeCard(
                            title = "Светлая",
                            isSelected = theme == AppTheme.LIGHT,
                            previewColor = Color(0xFFF5F5F5),
                            onClick = { viewModel.setTheme(AppTheme.LIGHT) }
                        )
                        ThemeCard(
                            title = "AMOLED",
                            isSelected = theme == AppTheme.AMOLED,
                            previewColor = Color.Black,
                            onClick = { viewModel.setTheme(AppTheme.AMOLED) }
                        )
                    }
                }
                SettingsDivider()
                SettingsToggleRow(
                    title = "Отправка по Enter",
                    subtitle = "Enter отправляет, Shift+Enter — перенос",
                    checked = enterSends,
                    onCheckedChange = { viewModel.setEnterSends(it) }
                )
            }

            // About
            SettingsSectionTitle("О приложении")
            SettingsSection {
                SettingsRow(
                    icon = Icons.Default.Info,
                    iconColor = Color(0xFF8E8E93),
                    title = "О приложении",
                    onClick = { /* Show about dialog */ }
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Default.Code,
                    iconColor = Color(0xFFFF6B00),
                    title = "Для разработчиков",
                    onClick = { /* Show dev info */ }
                )
            }

            // Logout
            SettingsSection {
                Button(
                    onClick = { viewModel.logout() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Выйти")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
    )
}

@Composable
private fun SettingsSection(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(content = content)
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            color = iconColor,
            shape = CircleShape
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.padding(8.dp), tint = Color.White)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(title, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

@Composable
private fun ThemeCard(
    title: String,
    isSelected: Boolean,
    previewColor: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            color = previewColor,
            shape = RoundedCornerShape(8.dp)
        ) {
            // Mini chat preview
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
                    Surface(modifier = Modifier.width(28.dp).height(4.dp), color = Color.Gray.copy(alpha = 0.3f), shape = RoundedCornerShape(2.dp)) {}
                    Spacer(modifier = Modifier.height(3.dp))
                    Surface(modifier = Modifier.width(20.dp).height(4.dp), color = Color.Gray.copy(alpha = 0.2f), shape = RoundedCornerShape(2.dp)) {}
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            title,
            fontSize = 11.sp,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
