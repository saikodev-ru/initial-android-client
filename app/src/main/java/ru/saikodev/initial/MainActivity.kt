package ru.saikodev.initial

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import ru.saikodev.initial.domain.model.User
import ru.saikodev.initial.ui.navigation.AppNavigation
import ru.saikodev.initial.ui.settings.SettingsViewModel
import ru.saikodev.initial.ui.theme.AppTheme
import ru.saikodev.initial.ui.theme.InitialTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isLoggedIn = checkAuthToken()

            InitialTheme(theme = AppTheme.DARK) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(
                        isLoggedIn = isLoggedIn,
                        onAuthSuccess = { }
                    )
                }
            }
        }
    }

    private fun checkAuthToken(): Boolean {
        val prefs = getSharedPreferences("initial_prefs", MODE_PRIVATE)
        return prefs.getString("auth_token", null) != null
    }
}
