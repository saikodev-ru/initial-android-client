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
import ru.saikodev.initial.ui.auth.EmailLoginScreen
import ru.saikodev.initial.ui.auth.QrLoginScreen
import ru.saikodev.initial.ui.auth.CodeVerificationScreen
import ru.saikodev.initial.ui.auth.ProfileSetupScreen
import ru.saikodev.initial.ui.chatlist.ChatListScreen
import ru.saikodev.initial.ui.chat.ChatScreen
import ru.saikodev.initial.ui.settings.SettingsScreen
import ru.saikodev.initial.ui.settings.MainViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val mainViewModel: MainViewModel = hiltViewModel()
    val isLoggedIn by mainViewModel.isLoggedIn.collectAsState()
    val currentUser by mainViewModel.currentUser.collectAsState()

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) Screen.ChatList.route else Screen.Auth.route
    ) {
        // Auth
        composable(Screen.Auth.route) {
            AuthScreen(navController, mainViewModel)
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
                onSuccess = {
                    navController.navigate(Screen.ProfileSetup.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                },
                onLoggedIn = {
                    navController.navigate(Screen.ChatList.route) {
                        popUpTo(0) { inclusive = true }
                    }
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

        // Main
        composable(Screen.ChatList.route) {
            ChatListScreen(
                onChatClick = { chatId ->
                    navController.navigate(Screen.Chat.createRoute(chatId))
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
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

@Composable
fun AuthScreen(navController: androidx.navigation.NavController, mainViewModel: MainViewModel) {
    // This will be a screen with tabs: QR Login and Email Login
    androidx.compose.foundation.layout.Box(
        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.material3.Text("Auth Splash - check login status")
    }

    // Auto-navigate based on auth state
    // For now, just show the QR login screen
    QrLoginScreen(
        onLoginSuccess = {
            navController.navigate(Screen.ChatList.route) {
                popUpTo(0) { inclusive = true }
            }
        },
        onEmailLogin = {
            navController.navigate(Screen.EmailLogin.route)
        }
    )
}
