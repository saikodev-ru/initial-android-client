package ru.saikodev.initial.ui.auth

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import ru.saikodev.initial.domain.model.User
import ru.saikodev.initial.util.QrCodeUtils

/**
 * QR Login screen.
 * Shows QR code for scanning, with email login fallback button.
 * After QR approval, fetches user profile and passes to callback.
 */
@Composable
fun QrLoginScreen(
    onLoginSuccess: (user: User?) -> Unit,
    onEmailLogin: () -> Unit,
    onQrScan: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val qrUrl by viewModel.qrUrl.collectAsState()
    val qrStatus by viewModel.qrStatus.collectAsState()
    val qrApprovedUser by viewModel.qrApprovedUser.collectAsState()
    val error by viewModel.error.collectAsState()
    val loginSuccess by rememberUpdatedState(qrStatus is AuthViewModel.QrStatus.Approved)

    // Navigate after QR approved and user fetched (with timeout fallback)
    LaunchedEffect(loginSuccess) {
        if (loginSuccess) {
            // Wait for user fetch, but don't block indefinitely
            delay(1500)
            onLoginSuccess(qrApprovedUser)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.startQrFlow()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ─── Logo ───
            Text(
                text = "Initial.",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ─── Subtitle ───
            Text(
                text = "Быстрый вход через QR-код",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ─── QR Code Display ───
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                when (qrStatus) {
                    is AuthViewModel.QrStatus.Loading -> {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                    is AuthViewModel.QrStatus.Ready -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (qrUrl != null) {
                                val qrBitmap = remember(qrUrl) {
                                    mutableStateOf(null as Bitmap?)
                                }
                                LaunchedEffect(qrUrl) {
                                    withContext(Dispatchers.IO) {
                                        val bmp = QrCodeUtils.generateQrBitmap(qrUrl!!, 800, 32)
                                        withContext(Dispatchers.Main) {
                                            qrBitmap.value = bmp
                                        }
                                    }
                                }
                                if (qrBitmap.value != null) {
                                    androidx.compose.foundation.Image(
                                        bitmap = qrBitmap.value!!.asImageBitmap(),
                                        contentDescription = "QR Code",
                                        modifier = Modifier
                                            .size(220.dp)
                                            .padding(8.dp)
                                    )
                                } else {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                }
                            } else {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    is AuthViewModel.QrStatus.Scanned -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(40.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Подтвердите на телефоне",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    is AuthViewModel.QrStatus.Approved -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Вход…",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    is AuthViewModel.QrStatus.Expired -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "QR-код устарел",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                    is AuthViewModel.QrStatus.Error -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                (qrStatus as AuthViewModel.QrStatus.Error).message,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ─── Status text below QR ───
            Text(
                when (qrStatus) {
                    is AuthViewModel.QrStatus.Loading -> "Загрузка QR-кода…"
                    is AuthViewModel.QrStatus.Ready -> "Отсканируйте код в приложении"
                    is AuthViewModel.QrStatus.Scanned -> "Ожидание подтверждения…"
                    is AuthViewModel.QrStatus.Approved -> "Добро пожаловать!"
                    is AuthViewModel.QrStatus.Expired -> "Обновление QR-кода…"
                    is AuthViewModel.QrStatus.Error -> "Произошла ошибка"
                },
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                "Обновляется каждые 3 минуты",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            if (error != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    error!!,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ─── Email Login Button ───
            TextButton(
                onClick = onEmailLogin
            ) {
                Icon(
                    Icons.Rounded.Mail,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Войти по Email",
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // ─── Scan QR Button ───
            TextButton(
                onClick = onQrScan
            ) {
                Icon(
                    Icons.Rounded.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Сканировать QR-код",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
