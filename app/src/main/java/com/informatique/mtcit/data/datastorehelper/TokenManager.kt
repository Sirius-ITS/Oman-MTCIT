package com.informatique.mtcit.data.datastorehelper

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "user_prefs")

object TokenManager {
    private val TOKEN_KEY = stringPreferencesKey("token_key")
    private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
    private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
    private val TOKEN_TYPE_KEY = stringPreferencesKey("token_type")
    private val EXPIRES_IN_KEY = longPreferencesKey("expires_in")
    private val TOKEN_TIMESTAMP_KEY = longPreferencesKey("token_timestamp")

    // Legacy token methods (kept for backward compatibility)
    suspend fun saveToken(context: Context, token: String) {
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
        }
    }

    suspend fun getToken(context: Context): String? {
        return context.dataStore.data
            .map { prefs -> prefs[TOKEN_KEY] }
            .first()
    }

    // OAuth token methods
    suspend fun saveOAuthTokens(
        context: Context,
        accessToken: String,
        refreshToken: String? = null,
        tokenType: String = "Bearer",
        expiresIn: Long? = null
    ) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN_KEY] = accessToken
            refreshToken?.let { prefs[REFRESH_TOKEN_KEY] = it }
            prefs[TOKEN_TYPE_KEY] = tokenType
            expiresIn?.let { prefs[EXPIRES_IN_KEY] = it }
            prefs[TOKEN_TIMESTAMP_KEY] = System.currentTimeMillis()
        }
    }

    suspend fun getAccessToken(context: Context): String? {
        return context.dataStore.data
            .map { prefs -> prefs[ACCESS_TOKEN_KEY] }
            .first()
    }

    suspend fun getRefreshToken(context: Context): String? {
        return context.dataStore.data
            .map { prefs -> prefs[REFRESH_TOKEN_KEY] }
            .first()
    }

    suspend fun getTokenType(context: Context): String? {
        return context.dataStore.data
            .map { prefs -> prefs[TOKEN_TYPE_KEY] }
            .first()
    }

    suspend fun isTokenExpired(context: Context): Boolean {
        val prefs = context.dataStore.data.first()
        val expiresIn = prefs[EXPIRES_IN_KEY] ?: return true
        val timestamp = prefs[TOKEN_TIMESTAMP_KEY] ?: return true
        val expirationTime = timestamp + (expiresIn * 1000) // Convert seconds to milliseconds
        return System.currentTimeMillis() >= expirationTime
    }

    suspend fun clearToken(context: Context) {
        context.dataStore.edit {
            it.remove(TOKEN_KEY)
            it.remove(ACCESS_TOKEN_KEY)
            it.remove(REFRESH_TOKEN_KEY)
            it.remove(TOKEN_TYPE_KEY)
            it.remove(EXPIRES_IN_KEY)
            it.remove(TOKEN_TIMESTAMP_KEY)
        }
    }
}
