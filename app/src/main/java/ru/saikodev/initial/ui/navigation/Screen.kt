package ru.saikodev.initial.ui.navigation

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object EmailLogin : Screen("email_login")
    object CodeVerification : Screen("code_verification/{email}/{via}") {
        fun createRoute(email: String, via: String) = "code_verification/$email/$via"
    }
    object ProfileSetup : Screen("profile_setup")
    object ChatList : Screen("chat_list")
    object Chat : Screen("chat/{chatId}") {
        fun createRoute(chatId: Int) = "chat/$chatId"
    }
    object Settings : Screen("settings")
    object ProfileEdit : Screen("profile_edit")
    object SettingsNotifications : Screen("settings_notifications")
    object SettingsChat : Screen("settings_chat")
    object SettingsDevices : Screen("settings_devices")
    object SettingsAbout : Screen("settings_about")
}
