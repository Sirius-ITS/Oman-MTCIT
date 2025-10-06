package com.informatique.mtcit.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.informatique.mtcit.ui.theme.ThemeOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore("user_preferences")

object ThemePreferences {
    private val THEME_KEY = stringPreferencesKey("theme_option")

    suspend fun saveTheme(context: Context, theme: ThemeOption) {
        context.dataStore.edit { prefs ->
            prefs[THEME_KEY] = theme.name
        }
    }

    fun getTheme(context: Context): Flow<ThemeOption> =
        context.dataStore.data.map { prefs ->
            val name = prefs[THEME_KEY]
            ThemeOption.values().firstOrNull { it.name == name } ?: ThemeOption.SYSTEM_DEFAULT
        }
}
