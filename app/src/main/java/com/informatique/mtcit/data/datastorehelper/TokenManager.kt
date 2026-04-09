package com.informatique.mtcit.data.datastorehelper

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

val Context.dataStore by preferencesDataStore(name = "user_prefs")

object TokenManager {
    private val TOKEN_KEY = stringPreferencesKey("token_key")
    private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
    private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
    private val TOKEN_TYPE_KEY = stringPreferencesKey("token_type")
    private val EXPIRES_IN_KEY = longPreferencesKey("expires_in")
    private val TOKEN_TIMESTAMP_KEY = longPreferencesKey("token_timestamp")
    private val CIVIL_ID_KEY = stringPreferencesKey("civil_id")
    private val FCM_TOKEN_KEY = stringPreferencesKey("fcm_token")
    private val FCM_REGISTERED_KEY = booleanPreferencesKey("fcm_registered")
    private val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")

    // FCM token methods
    suspend fun saveFcmToken(context: Context, token: String) {
        context.dataStore.edit { prefs ->
            prefs[FCM_TOKEN_KEY] = token
        }
        Log.d("TokenManager", "✅ FCM token saved")
    }

    suspend fun getFcmToken(context: Context): String? {
        return context.dataStore.data
            .map { prefs -> prefs[FCM_TOKEN_KEY] }
            .first()
    }

    /** Persist whether the FCM token is currently registered on the server. */
    suspend fun saveFcmRegistered(context: Context, registered: Boolean) {
        context.dataStore.edit { prefs -> prefs[FCM_REGISTERED_KEY] = registered }
        Log.d("TokenManager", "✅ FCM registered state saved: $registered")
    }

    /** Returns true if the FCM token has already been registered on the server. */
    suspend fun isFcmRegistered(context: Context): Boolean {
        return context.dataStore.data
            .map { prefs -> prefs[FCM_REGISTERED_KEY] ?: false }
            .first()
    }

    /** Persist the user's notification preference (switch state). Defaults to true. */
    suspend fun saveNotificationsEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[NOTIFICATIONS_ENABLED_KEY] = enabled }
        Log.d("TokenManager", "✅ Notifications enabled state saved: $enabled")
    }

    /** Returns true if the user has notifications enabled (default true when never set). */
    suspend fun isNotificationsEnabled(context: Context): Boolean {
        return context.dataStore.data
            .map { prefs -> prefs[NOTIFICATIONS_ENABLED_KEY] ?: true }
            .first()
    }

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

    suspend fun saveCivilId(context: Context, civilId: String) {
        context.dataStore.edit { prefs ->
            prefs[CIVIL_ID_KEY] = civilId
        }
        Log.d("TokenManager", "✅ Civil ID persisted to DataStore: $civilId")
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
        Log.d("TokenManager", "🗑️ Starting token clear operation...")

        // Log tokens before clearing
        val prefs = context.dataStore.data.first()
        Log.d("TokenManager", "📋 Tokens before clear:")
        Log.d("TokenManager", "  - TOKEN_KEY exists: ${prefs[TOKEN_KEY] != null}")
        Log.d("TokenManager", "  - ACCESS_TOKEN_KEY exists: ${prefs[ACCESS_TOKEN_KEY] != null}")
        Log.d("TokenManager", "  - REFRESH_TOKEN_KEY exists: ${prefs[REFRESH_TOKEN_KEY] != null}")
        Log.d("TokenManager", "  - TOKEN_TYPE_KEY exists: ${prefs[TOKEN_TYPE_KEY] != null}")
        Log.d("TokenManager", "  - EXPIRES_IN_KEY exists: ${prefs[EXPIRES_IN_KEY] != null}")
        Log.d("TokenManager", "  - TOKEN_TIMESTAMP_KEY exists: ${prefs[TOKEN_TIMESTAMP_KEY] != null}")

        // Clear all tokens
        context.dataStore.edit { preferences ->
            preferences.remove(TOKEN_KEY)
            preferences.remove(ACCESS_TOKEN_KEY)
            preferences.remove(REFRESH_TOKEN_KEY)
            preferences.remove(TOKEN_TYPE_KEY)
            preferences.remove(EXPIRES_IN_KEY)
            preferences.remove(TOKEN_TIMESTAMP_KEY)
            preferences.remove(CIVIL_ID_KEY)
            preferences.clear() // Clear everything to be sure
        }

        // Verify tokens were cleared
        val prefsAfter = context.dataStore.data.first()
        Log.d("TokenManager", "📋 Tokens after clear:")
        Log.d("TokenManager", "  - TOKEN_KEY exists: ${prefsAfter[TOKEN_KEY] != null}")
        Log.d("TokenManager", "  - ACCESS_TOKEN_KEY exists: ${prefsAfter[ACCESS_TOKEN_KEY] != null}")
        Log.d("TokenManager", "  - REFRESH_TOKEN_KEY exists: ${prefsAfter[REFRESH_TOKEN_KEY] != null}")
        Log.d("TokenManager", "  - All preferences count: ${prefsAfter.asMap().size}")

        Log.d("TokenManager", "✅ Token clear operation completed!")
    }

    /**
     * Decode JWT token and extract payload as JSON
     */
    private fun decodeJwtPayload(token: String): JSONObject? {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) {
                Log.e("TokenManager", "Invalid JWT token format")
                return null
            }

            // Decode the payload (second part)
            val payload = parts[1]
            val decodedBytes = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP)
            val decodedString = String(decodedBytes, Charsets.UTF_8)

            JSONObject(decodedString)
        } catch (e: Exception) {
            Log.e("TokenManager", "Failed to decode JWT token: ${e.message}", e)
            null
        }
    }

    /**
     * Get user name from token
     * Supports both old token format (name/given_name/family_name) and
     * new token format (FullNameArabic/FullNameEnglish)
     */
    suspend fun getUserName(context: Context): String? {
        val token = getAccessToken(context) ?: return null
        val payload = decodeJwtPayload(token)
        // Try new field names first (mtimedevidp), then fall back to old field names (omankeycloak)
        return payload?.optString("FullNameArabic")?.takeIf { it.isNotEmpty() }
            ?: payload?.optString("FullNameEnglish")?.takeIf { it.isNotEmpty() }
            ?: payload?.optString("name")?.takeIf { it.isNotEmpty() }
    }

    /**
     * Get civil ID from token (this will be used as ownerId)
     */
    suspend fun getCivilId(context: Context): String? {
        // ✅ First: check the value persisted from userinfo endpoint (most reliable)
        val storedCivilId = context.dataStore.data
            .map { prefs -> prefs[CIVIL_ID_KEY] }
            .first()
        if (!storedCivilId.isNullOrEmpty()) {
            Log.d("TokenManager", "✅ Civil ID from DataStore: $storedCivilId")
            return storedCivilId
        }

        val token = getAccessToken(context) ?: run {
            Log.w("TokenManager", "⚠️ No access token found when getting civil ID")
            return null
        }

        val payload = decodeJwtPayload(token)
        if (payload == null) {
            Log.e("TokenManager", "❌ Failed to decode JWT payload")
            return null
        }

        // Log all available fields in the token
        Log.d("TokenManager", "📋 JWT Token fields: ${payload.keys().asSequence().toList()}")

        // Try different possible field names for civil ID in the JWT payload
        val civilId = payload.optString("civilId")?.takeIf { it.isNotEmpty() }
            ?: payload.optString("civil_id")?.takeIf { it.isNotEmpty() }

        if (civilId != null) {
            Log.d("TokenManager", "✅ Found civil ID in JWT payload: $civilId")
        } else {
            Log.w("TokenManager", "⚠️ No civil ID found in token or DataStore. Available JWT fields: ${payload.keys().asSequence().toList()}")
        }

        return civilId
    }

    /**
     * Get email from token
     */
    suspend fun getUserEmail(context: Context): String? {
        val token = getAccessToken(context) ?: return null
        val payload = decodeJwtPayload(token)
        return payload?.optString("email")?.takeIf { it.isNotEmpty() }
    }

    /**
     * Get preferred username from token
     */
    suspend fun getPreferredUsername(context: Context): String? {
        val token = getAccessToken(context) ?: return null
        val payload = decodeJwtPayload(token)
        return payload?.optString("preferred_username")?.takeIf { it.isNotEmpty() }
    }

    /**
     * Get all user data from token
     * Supports both old token format (name/given_name/family_name) and
     * new token format (FullNameArabic/FullNameEnglish)
     */
    suspend fun getUserData(context: Context): UserData? {
        val token = getAccessToken(context) ?: return null
        val payload = decodeJwtPayload(token) ?: return null

        return try {
            // New token fields (mtimedevidp.mtcit.gov.om)
            val fullNameArabic = payload.optString("FullNameArabic", "")
            val fullNameEnglish = payload.optString("FullNameEnglish", "")
            // Old token fields (omankeycloak) - kept for backward compatibility
            val name = payload.optString("name", "")
            val givenName = payload.optString("given_name", "")
            val familyName = payload.optString("family_name", "")

            // Prefer new fields, fall back to old
            val resolvedName = fullNameArabic.takeIf { it.isNotEmpty() }
                ?: fullNameEnglish.takeIf { it.isNotEmpty() }
                ?: name

            UserData(
                name = resolvedName,
                civilId = payload.optString("civilId", ""),
                email = payload.optString("email", ""),
                preferredUsername = payload.optString("preferred_username", ""),
                givenName = givenName,
                familyName = familyName,
                fullNameArabic = fullNameArabic,
                fullNameEnglish = fullNameEnglish
            )
        } catch (e: Exception) {
            Log.e("TokenManager", "Failed to extract user data: ${e.message}", e)
            null
        }
    }

    /**
     * Get user role from JWT token (realm_access.roles)
     */
    suspend fun getUserRole(context: Context): String? {
        val token = getAccessToken(context) ?: run {
            Log.w("TokenManager", "⚠️ No access token found when getting user role")
            return null
        }

        val payload = decodeJwtPayload(token)
        if (payload == null) {
            Log.e("TokenManager", "❌ Failed to decode JWT payload for role extraction")
            return null
        }

        return try {
            // Extract realm_access.roles from JWT
            val realmAccess = payload.optJSONObject("realm_access")
            if (realmAccess != null) {
                val rolesArray = realmAccess.optJSONArray("roles")
                if (rolesArray != null && rolesArray.length() > 0) {
                    // Return the first role (typically "client" or "engineer")
                    var foundRole = ""
                    for (i in 0 until rolesArray.length()) {
                        Log.d("TokenManager", "🔍 Found role in JWT: ${rolesArray.getString(i)}")
                        if (rolesArray.getString(i) != null) foundRole = rolesArray.getString(i)
                        if (foundRole.equals("engineer", ignoreCase = true)) break
                    }
                    Log.d("TokenManager", "✅ Found user role: $foundRole")
                    return foundRole
                }
            }

            Log.w("TokenManager", "⚠️ No realm_access.roles found in token")
            null
        } catch (e: Exception) {
            Log.e("TokenManager", "❌ Failed to extract user role: ${e.message}", e)
            null
        }
    }

    /**
     * Check if user is an engineer
     */
    suspend fun isEngineer(context: Context): Boolean {
        val role = getUserRole(context)
        return role?.equals("engineer", ignoreCase = true) == true
    }

    /**
     * Check if user is a client
     */
    suspend fun isClient(context: Context): Boolean {
        val role = getUserRole(context)
        return role?.equals("client", ignoreCase = true) == true
    }

    /**
     * Data class to hold user information from JWT token
     * Supports both old format (name/given_name/family_name) and
     * new format (FullNameArabic/FullNameEnglish)
     */
    data class UserData(
        val name: String,
        val civilId: String,
        val email: String,
        val preferredUsername: String,
        val givenName: String,
        val familyName: String,
        val fullNameArabic: String = "",
        val fullNameEnglish: String = ""
    )
}
