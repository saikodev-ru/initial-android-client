package ru.saikodev.initial.ui.navigation

sealed class Screen(val route: String) {
    data object Auth : Screen("auth")
    data object EmailLogin : Screen("email_login")
    data object CodeVerification : Screen("code_verification/{email}/{via}") {
        fun createRoute(email: String, via: String) = "code_verification/$email/$via"
    }
    data object ProfileSetup : Screen("profile_setup/{email}") {
        fun createRoute(email: String) = "profile_setup/$email"
    }
    data object QrScanner : Screen("qr_scanner?loginToken={loginToken}&linkToken={linkToken}") {
        fun createRoute(loginToken: String? = null, linkToken: String? = null): String {
            val params = mutableListOf<String>()
            loginToken?.let { params.add("loginToken=$it") }
            linkToken?.let { params.add("linkToken=$it") }
            return if (params.isEmpty()) "qr_scanner" else "qr_scanner?${params.joinToString("&")}"
        }
    }
    data object ChatList : Screen("chat_list")
    data object Chat : Screen("chat/{chatId}/{signalId}/{partnerName}") {
        fun createRoute(chatId: Int, signalId: String? = null, partnerName: String? = null): String {
            val name = partnerName?.let { java.net.URLEncoder.encode(it, "UTF-8") } ?: "null"
            val sid = signalId ?: "null"
            return "chat/$chatId/$sid/$name"
        }
    }
    data object Settings : Screen("settings")
}
