package ru.saikodev.initial.ui.auth

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import ru.saikodev.initial.domain.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeVerificationScreen(
    email: String,
    via: String,
    onVerified: (user: User) -> Unit,
    onBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val code = remember { mutableStateListOf("", "", "", "", "") }
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val resendTimer by viewModel.resendTimer.collectAsState()
    val focusManager = LocalFocusManager.current
    val focusRequesters = remember { List(5) { FocusRequester() } }

    LaunchedEffect(Unit) {
        viewModel.startResendTimer()
        delay(100)
        focusRequesters[0].requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .padding(top = 32.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Назад")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (via == "signal") "Код в Signal" else "Проверьте почту",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (via == "signal") "Отправили код в чат с @initial"
                else "Отправили 5-значный код на $email",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (error != null) {
                Text(
                    text = error!!,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ─── Code input boxes ───
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(5) { index ->
                    OutlinedTextField(
                        value = code[index],
                        onValueChange = { value ->
                            if (value.length <= 1) {
                                code[index] = value
                                if (value.isNotEmpty() && index < 4) {
                                    focusRequesters[index + 1].requestFocus()
                                }
                                // Auto-submit when all filled
                                if (code.all { it.isNotEmpty() }) {
                                    focusManager.clearFocus()
                                    viewModel.verifyCode(email, code.joinToString("")) { user ->
                                        onVerified(user)
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequesters[index]),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            textAlign = TextAlign.Center,
                            fontSize = 22.sp
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ─── Verify button ───
            Button(
                onClick = {
                    viewModel.verifyCode(email, code.joinToString("")) { user ->
                        onVerified(user)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = code.all { it.isNotEmpty() } && !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Подтвердить", fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ─── Resend ───
            if (resendTimer > 0) {
                Text(
                    "Повторная отправка через ${resendTimer}с",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                TextButton(onClick = {
                    viewModel.sendCode(email) { _, _ ->
                        viewModel.startResendTimer()
                    }
                }) {
                    Text("Отправить код повторно")
                }
            }
        }
    }
}
