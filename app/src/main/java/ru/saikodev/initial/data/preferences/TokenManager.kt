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
    private val prefs: SharedPreferences = context.getSharedPreferences("initial_prefs", Context.MODE_PRIVATE)

    fun saveToken(token: String) = prefs.edit().putString("auth_token", token).apply()
    fun getToken(): String? = prefs.getString("auth_token", null)
    fun saveUserJson(json: String) = prefs.edit().putString("user_json", json).apply()
    fun getUserJson(): String? = prefs.getString("user_json", null)
    fun saveSetting(key: String, value: String) = prefs.edit().putString(key, value).apply()
    fun getSetting(key: String, default: String? = null): String? = prefs.getString(key, default)
    fun saveSettingInt(key: String, value: Int) = prefs.edit().putInt(key, value).apply()
    fun getSettingInt(key: String, default: Int = 0): Int = prefs.getInt(key, default)
    fun saveSettingBool(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()
    fun getSettingBool(key: String, default: Boolean = false): Boolean = prefs.getBoolean(key, default)
    fun clearAll() = prefs.edit().clear().apply()
}
