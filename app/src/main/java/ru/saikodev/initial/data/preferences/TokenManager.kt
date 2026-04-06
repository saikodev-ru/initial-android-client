package ru.saikodev.initial.data.preferences

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("initial_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER_JSON = "user_json"
    }

    fun saveToken(token: String) = prefs.edit().putString(KEY_TOKEN, token).apply()

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun clearToken() = prefs.edit().remove(KEY_TOKEN).apply()

    fun saveUserJson(json: String) = prefs.edit().putString(KEY_USER_JSON, json).apply()

    fun getUserJson(): String? = prefs.getString(KEY_USER_JSON, null)

    fun clearAll() = prefs.edit().clear().apply()
}
