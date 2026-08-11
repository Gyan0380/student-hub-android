package com.studenthub.app.util

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(name = "studenthub_theme")

enum class AppThemeOption { LIGHT, DARK, SEPIA, OCEAN }

/**
 * Native port of the web app's theme picker (Light/Dark/Sepia/Ocean), persisted via
 * DataStore instead of localStorage. One value app-wide (matches website behaviour —
 * theme isn't per-room or per-user-scoped there either).
 */
class ThemeStore(private val context: Context) {
    private val key = stringPreferencesKey("theme")

    val theme: Flow<AppThemeOption> = context.themeDataStore.data.map { prefs ->
        runCatching { AppThemeOption.valueOf(prefs[key] ?: "LIGHT") }.getOrDefault(AppThemeOption.LIGHT)
    }

    suspend fun setTheme(option: AppThemeOption) {
        context.themeDataStore.edit { prefs -> prefs[key] = option.name }
    }
}
