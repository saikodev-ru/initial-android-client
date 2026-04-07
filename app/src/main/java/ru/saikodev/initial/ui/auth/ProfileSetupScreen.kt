package ru.saikodev.initial.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.saikodev.initial.domain.model.User

@Composable
fun ProfileSetupScreen(
    viewModel: AuthViewModel,
    email: String,
    onProfileCreated: (User) -> Unit
) {
    var nickname by remember { mutableStateOf("") }
    var signalId by remember { mutableStateOf("") }
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val verifiedUser by viewModel.verifiedUser.collectAsStateWithLifecycle()

    val isValidNickname = nickname.isNotBlank() && nickname.length >= 2
    val isValidSignalId = signalId.isBlank() || (signalId.length >= 3)
    val isFormValid = isValidNickname && isValidSignalId

    LaunchedEffect(verifiedUser) {
        verifiedUser?.let { onProfileCreated(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(64.dp))

        Text(
            text = "Создайте профиль",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Введите имя и Initial ID для вашего аккаунта",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Nickname field
        OutlinedTextField(
            value = nickname,
            onValueChange = {
                if (it.length <= 64) {
                    nickname = it
                }
                viewModel.clearError()
            },
            label = { Text("Имя") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            supportingText = {
                if (nickname.isNotEmpty() && nickname.length < 2) {
                    Text("Минимум 2 символа")
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Signal ID field
        OutlinedTextField(
            value = signalId,
            onValueChange = { value ->
                if (value.all { it.isLetterOrDigit() || it == '_' } && value.length <= 30) {
                    signalId = value
                }
                viewModel.clearError()
            },
            label = { Text("Initial ID") },
            prefix = { Text("@") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            supportingText = {
                if (signalId.isBlank()) {
                    Text("Необязательно")
                } else if (signalId.length < 3) {
                    Text("Минимум 3 символа (a-z, 0-9, _)")
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Email display (read-only context)
        Text(
            text = "Аккаунт: $email",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Error message
        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Submit button
        Button(
            onClick = {
                viewModel.createProfile(
                    nickname = nickname,
                    signalId = signalId.ifBlank { null },
                    email = email
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = isFormValid && !isLoading,
            shape = MaterialTheme.shapes.medium
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Продолжить", fontSize = 16.sp)
            }
        }
    }
}
