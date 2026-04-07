package ru.saikodev.initial.ui.navigation

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ru.saikodev.initial.ui.auth.*
import ru.saikodev.initial.ui.chatlist.ChatListScreen
import ru.saikodev.initial.ui.chatlist.ChatListViewModel
import ru.saikodev.initial.ui.chat.ChatScreen
import ru.saikodev.initial.ui.chat.ChatViewModel
import ru.saikodev.initial.ui.settings.SettingsScreen
import ru.saikodev.initial.ui.settings.SettingsViewModel
import ru.saikodev.initial.ui.components.QrScannerScreen
import ru.saikodev.initial.domain.model.User

@Composable
fun AppNavigation(
    isLoggedIn: Boolean,
    onAuthSuccess: (User) -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val startDestination = if (isLoggedIn) Screen.ChatList.route else Screen.Auth.route

    LaunchedEffect(currentRoute) {
        Log.d("Nav", "Current route: $currentRoute")
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
        popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) }
    ) {
        // ── Auth ──
        composable(Screen.Auth.route) {
            val viewModel: AuthViewModel = hiltViewModel()
            AuthScreen(
                viewModel = viewModel,
                onEmailLogin = { navController.navigate(Screen.EmailLogin.route) },
                onQrScan = { loginToken, linkToken ->
                    navController.navigate(Screen.QrScanner.createRoute(loginToken, linkToken))
                },
                onAuthSuccess = { user ->
                    if (user.signalId.isNullOrBlank()) {
                        navController.navigate(Screen.ProfileSetup.createRoute(user.email))
                    } else {
                        navController.navigate(Screen.ChatList.route) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    }
                    onAuthSuccess(user)
                }
            )
        }

        // ── Email Login ──
        composable(Screen.EmailLogin.route) {
            val viewModel: AuthViewModel = hiltViewModel()
            EmailLoginScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onCodeSent = { email, via ->
                    navController.navigate(Screen.CodeVerification.createRoute(email, via)) {
                        popUpTo(Screen.Auth.route)
                    }
                }
            )
        }

        // ── Code Verification ──
        composable(
            route = Screen.CodeVerification.route,
            arguments = listOf(
                navArgument("email") { type = NavType.StringType },
                navArgument("via") { type = NavType.StringType }
            )
        ) {
            val email = it.arguments?.getString("email") ?: ""
            val viewModel: AuthViewModel = hiltViewModel()
            CodeVerificationScreen(
                viewModel = viewModel,
                email = email,
                onBack = { navController.popBackStack() },
                onVerified = { user ->
                    if (user.signalId.isNullOrBlank()) {
                        navController.navigate(Screen.ProfileSetup.createRoute(user.email)) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.ChatList.route) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                        onAuthSuccess(user)
                    }
                }
            )
        }

        // ── Profile Setup ──
        composable(
            route = Screen.ProfileSetup.route,
            arguments = listOf(navArgument("email") { type = NavType.StringType })
        ) {
            val email = it.arguments?.getString("email") ?: ""
            val viewModel: AuthViewModel = hiltViewModel()
            ProfileSetupScreen(
                viewModel = viewModel,
                email = email,
                onProfileCreated = { user ->
                    navController.navigate(Screen.ChatList.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                    onAuthSuccess(user)
                }
            )
        }

        // ── QR Scanner ──
        composable(
            route = Screen.QrScanner.route,
            arguments = listOf(
                navArgument("loginToken") {
                    type = NavType.StringType
                    defaultValue = "null"
                    nullable = true
                },
                navArgument("linkToken") {
                    type = NavType.StringType
                    defaultValue = "null"
                    nullable = true
                }
            )
        ) {
            val loginToken = it.arguments?.getString("loginToken")?.takeIf { it != "null" }
            val linkToken = it.arguments?.getString("linkToken")?.takeIf { it != "null" }
            val viewModel: AuthViewModel = hiltViewModel()
            QrScannerScreen(
                viewModel = viewModel,
                loginToken = loginToken,
                linkToken = linkToken,
                onBack = { navController.popBackStack() },
                onAuthSuccess = { user ->
                    navController.navigate(Screen.ChatList.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                    onAuthSuccess(user)
                }
            )
        }

        // ── Chat List ──
        composable(Screen.ChatList.route) {
            val viewModel: ChatListViewModel = hiltViewModel()
            ChatListScreen(
                viewModel = viewModel,
                onChatClick = { chatId, signalId, partnerName ->
                    navController.navigate(Screen.Chat.createRoute(chatId, signalId, partnerName))
                },
                onNewChat = { signalId, partnerName ->
                    navController.navigate(Screen.Chat.createRoute(0, signalId, partnerName))
                },
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                onQrScan = { loginToken, linkToken ->
                    navController.navigate(Screen.QrScanner.createRoute(loginToken, linkToken))
                }
            )
        }

        // ── Chat ──
        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("chatId") { type = NavType.IntType },
                navArgument("signalId") {
                    type = NavType.StringType
                    defaultValue = "null"
                },
                navArgument("partnerName") {
                    type = NavType.StringType
                    defaultValue = "null"
                }
            )
        ) {
            val viewModel: ChatViewModel = hiltViewModel()
            ChatScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // ── Settings ──
        composable(Screen.Settings.route) {
            val viewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
