package ru.saikodev.initial.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ru.saikodev.initial.domain.model.User
import ru.saikodev.initial.ui.auth.CodeVerificationScreen
import ru.saikodev.initial.ui.auth.EmailLoginScreen
import ru.saikodev.initial.ui.auth.ProfileSetupScreen
import ru.saikodev.initial.ui.auth.QrLoginScreen
import ru.saikodev.initial.ui.chat.ChatScreen
import ru.saikodev.initial.ui.chatlist.ChatListScreen
import ru.saikodev.initial.ui.settings.MainViewModel
import ru.saikodev.initial.ui.settings.SettingsScreen

/**
 * Main navigation for the Initial messenger app.
 *
 * Navigation flow:
 * - Auth screen shows QrLoginScreen (which includes email login button)
 * - After QR approved: if user has signal_id → ChatList, else → ProfileSetup
 * - After email code verified: if user has signal_id → ChatList, else → ProfileSetup
 * - After profile setup → ChatList
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val mainViewModel: MainViewModel = hiltViewModel()
    val isLoggedIn by mainViewModel.isLoggedIn.collectAsState()

    // Helper: navigate based on whether user has signal_id
    fun navigateAfterAuth(user: User?) {
        if (user?.signalId != null && user.signalId.isNotBlank()) {
            // Existing user with signal_id → go to ChatList
            navController.navigate(Screen.ChatList.route) {
                popUpTo(0) { inclusive = true }
            }
        } else {
            // New user without signal_id → go to ProfileSetup
            navController.navigate(Screen.ProfileSetup.route) {
                popUpTo(Screen.Auth.route) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) Screen.ChatList.route else Screen.Auth.route
    ) {
        // ─── Auth ───
        composable(Screen.Auth.route) {
            QrLoginScreen(
                onLoginSuccess = { user ->
                    navigateAfterAuth(user)
                },
                onEmailLogin = {
                    navController.navigate(Screen.EmailLogin.route)
                }
            )
        }

        composable(Screen.EmailLogin.route) {
            EmailLoginScreen(
                onCodeSent = { email, via ->
                    navController.navigate(Screen.CodeVerification.createRoute(email, via))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.CodeVerification.route,
            arguments = listOf(
                navArgument("email") { type = NavType.StringType },
                navArgument("via") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            val via = backStackEntry.arguments?.getString("via") ?: "email"
            CodeVerificationScreen(
                email = email,
                via = via,
                onVerified = { user ->
                    navigateAfterAuth(user)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ProfileSetup.route) {
            ProfileSetupScreen(
                onComplete = {
                    navController.navigate(Screen.ChatList.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ─── Main ───
        composable(Screen.ChatList.route) {
            ChatListScreen(
                onChatClick = { chatId ->
                    navController.navigate(Screen.Chat.createRoute(chatId))
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
                onNewChat = {
                    // TODO: Open new chat dialog / contact picker
                }
            )
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("chatId") { type = NavType.IntType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getInt("chatId") ?: 0
            ChatScreen(
                chatId = chatId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
