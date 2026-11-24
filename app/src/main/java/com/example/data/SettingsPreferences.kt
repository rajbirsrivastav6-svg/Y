package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "luma_settings")

enum class ThemeMode(val label: String) {
    DARK("Dark"),
    LIGHT("Light"),
    SYSTEM("System")
}

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val autoSpeak: Boolean = true,
    val speechRate: Float = 1.0f,
    val dynamicColor: Boolean = false,
    val handsFreeLoop: Boolean = true
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val AUTO_SPEAK = booleanPreferencesKey("auto_speak")
        val SPEECH_RATE = floatPreferencesKey("speech_rate")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val HANDS_FREE_LOOP = booleanPreferencesKey("hands_free_loop")
    }

    val preferencesFlow: Flow<UserPreferences> = context.settingsDataStore.data.map { prefs ->
        val modeName = prefs[Keys.THEME_MODE] ?: ThemeMode.DARK.name
        val mode = ThemeMode.entries.firstOrNull { it.name == modeName } ?: ThemeMode.DARK
        UserPreferences(
            themeMode = mode,
            autoSpeak = prefs[Keys.AUTO_SPEAK] ?: true,
            speechRate = (prefs[Keys.SPEECH_RATE] ?: 1.0f).coerceIn(0.6f, 1.6f),
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: false,
            handsFreeLoop = prefs[Keys.HANDS_FREE_LOOP] ?: true
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setAutoSpeak(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.AUTO_SPEAK] = enabled }
    }

    suspend fun setSpeechRate(rate: Float) {
        context.settingsDataStore.edit { it[Keys.SPEECH_RATE] = rate.coerceIn(0.6f, 1.6f) }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setHandsFreeLoop(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.HANDS_FREE_LOOP] = enabled }
    }
}
