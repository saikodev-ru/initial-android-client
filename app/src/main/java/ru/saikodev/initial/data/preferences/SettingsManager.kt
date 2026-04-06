package ru.saikodev.initial.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

enum class AppTheme { DARK, LIGHT, AMOLED }

@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notif_enabled")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val ANON_NOTIFICATIONS = booleanPreferencesKey("anon_notif")
        val ENTER_SENDS = booleanPreferencesKey("enter_sends")
        val QUICK_REPLY = booleanPreferencesKey("quick_reply")
        val CHAT_DIVIDERS = booleanPreferencesKey("chat_dividers")
        val FONT_SIZE = intPreferencesKey("font_size")
        val BG_PATTERN = booleanPreferencesKey("bg_pattern")
        val HDR_CENTER = booleanPreferencesKey("hdr_center")
        val HDR_HIDE_AV = booleanPreferencesKey("hdr_hide_av")
    }

    val theme: Flow<AppTheme> = context.dataStore.data.map { prefs ->
        when (prefs[Keys.THEME]) {
            "light" -> AppTheme.LIGHT
            "amoled" -> AppTheme.AMOLED
            else -> AppTheme.DARK
        }
    }

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.NOTIFICATIONS_ENABLED] ?: false }
    val soundEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.SOUND_ENABLED] ?: true }
    val anonNotifications: Flow<Boolean> = context.dataStore.data.map { it[Keys.ANON_NOTIFICATIONS] ?: false }
    val enterSends: Flow<Boolean> = context.dataStore.data.map { it[Keys.ENTER_SENDS] ?: true }
    val quickReply: Flow<Boolean> = context.dataStore.data.map { it[Keys.QUICK_REPLY] ?: true }
    val chatDividers: Flow<Boolean> = context.dataStore.data.map { it[Keys.CHAT_DIVIDERS] ?: true }
    val fontSize: Flow<Int> = context.dataStore.data.map { it[Keys.FONT_SIZE] ?: 15 }
    val bgPattern: Flow<Boolean> = context.dataStore.data.map { it[Keys.BG_PATTERN] ?: true }
    val hdrCenter: Flow<Boolean> = context.dataStore.data.map { it[Keys.HDR_CENTER] ?: false }
    val hdrHideAv: Flow<Boolean> = context.dataStore.data.map { it[Keys.HDR_HIDE_AV] ?: false }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { it[Keys.THEME] = theme.name.lowercase() }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun setAnonNotifications(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ANON_NOTIFICATIONS] = enabled }
    }

    suspend fun setEnterSends(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ENTER_SENDS] = enabled }
    }

    suspend fun setQuickReply(enabled: Boolean) {
        context.dataStore.edit { it[Keys.QUICK_REPLY] = enabled }
    }

    suspend fun setFontSize(size: Int) {
        context.dataStore.edit { it[Keys.FONT_SIZE] = size }
    }

    suspend fun setBgPattern(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BG_PATTERN] = enabled }
    }

    suspend fun setHdrCenter(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HDR_CENTER] = enabled }
    }

    suspend fun setHdrHideAv(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HDR_HIDE_AV] = enabled }
    }
}
