package ru.saikodev.initial.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.saikodev.initial.domain.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeVerificationScreen(
    viewModel: AuthViewModel,
    email: String,
    onBack: () -> Unit,
    onVerified: (User) -> Unit
) {
    val verifiedUser by viewModel.verifiedUser.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val resendTimer by viewModel.resendTimer.collectAsStateWithLifecycle()

    var code by remember { mutableStateOf(List(5) { "" }) }
    val focusRequesters = remember { List(5) { FocusRequester() } }
    var hasAttemptedSubmit by remember { mutableStateOf(false) }

    // Navigate on successful verification
    LaunchedEffect(verifiedUser) {
        verifiedUser?.let { onVerified(it) }
    }

    // Start resend timer on first composition
    LaunchedEffect(Unit) {
        focusRequesters[0].requestFocus()
        viewModel.startResendTimer()
    }

    // Auto-submit when all 5 digits are filled
    val fullCode = code.joinToString("")
    LaunchedEffect(fullCode) {
        if (fullCode.length == 5 && fullCode.all { it.isDigit() }) {
            viewModel.verifyCode(email, fullCode)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Введите код",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Мы отправили 5-значный код на $email",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 5-digit code input fields
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                code.forEachIndexed { index, digit ->
                    OutlinedTextField(
                        value = digit,
                        onValueChange = { value ->
                            if (value.length <= 1 && value.all { it.isDigit() }) {
                                val newList = code.toMutableList()
                                newList[index] = value
                                code = newList
                                if (value.isNotEmpty() && index < 4) {
                                    focusRequesters[index + 1].requestFocus()
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequesters[index]),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 24.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Error message
            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Loading indicator
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Resend code section
            if (resendTimer > 0) {
                Text(
                    text = "Отправить повторно через ${resendTimer}с",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                TextButton(onClick = { viewModel.resendCode(email, false) }) {
                    Text("Отправить код повторно", color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            TextButton(onClick = { viewModel.resendCode(email, true) }) {
                Text("Отправить на email", color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
