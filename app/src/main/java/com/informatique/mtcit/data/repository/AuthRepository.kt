package com.informatique.mtcit.data.repository

import android.content.Context
import android.util.Log
import com.informatique.mtcit.data.datastorehelper.TokenManager
import com.informatique.mtcit.data.model.oauth.OAuthTokenResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for handling OAuth authentication with Keycloak
 */
@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: HttpClient
) {
    companion object {
        private const val TAG = "AuthRepository"
        private const val TOKEN_ENDPOINT = "https://mtimedevidp.mtcit.gov.om/realms/oman/protocol/openid-connect/token"
        private const val USERINFO_ENDPOINT = "https://mtimedevidp.mtcit.gov.om/realms/oman/protocol/openid-connect/userinfo"
        private const val CLIENT_ID = "front"
        private const val REDIRECT_URI = "https://mtimedev.mtcit.gov.om/auth/callback"
    }

    /**
     * Exchange authorization code for access token.
     * @param codeVerifier PKCE code verifier (pass from LoginViewModel.lastCodeVerifier).
     */
    suspend fun exchangeCodeForToken(authorizationCode: String, codeVerifier: String = ""): Result<OAuthTokenResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🔄 Exchanging authorization code for token...")

                // ✅ FIX: Get the full HTTP response to check status code before deserializing
                val httpResponse = httpClient.submitForm(
                    url = TOKEN_ENDPOINT,
                    formParameters = Parameters.build {
                        append("grant_type", "authorization_code")
                        append("code", authorizationCode)
                        append("client_id", CLIENT_ID)
                        append("redirect_uri", REDIRECT_URI)
                        if (codeVerifier.isNotEmpty()) {
                            append("code_verifier", codeVerifier)
                        }
                    }
                )

                // ✅ FIX: Check status code before deserializing to avoid crash on error responses
                if (httpResponse.status.value != 200) {
                    val errorBody = try { httpResponse.bodyAsText() } catch (e: Exception) { "Unknown error" }
                    Log.e(TAG, "❌ Token exchange failed with status ${httpResponse.status.value}: $errorBody")
                    val errorMessage = when {
                        errorBody.contains("Code not valid") || errorBody.contains("invalid_grant") ->
                            "رمز التفويض غير صالح أو منتهي الصلاحية. يرجى تسجيل الدخول مرة أخرى"
                        errorBody.contains("PKCE verification failed") ->
                            "فشل التحقق من PKCE. يرجى المحاولة مرة أخرى"
                        else -> "فشل تبادل الرمز. يرجى تسجيل الدخول مرة أخرى"
                    }
                    return@withContext Result.failure(Exception(errorMessage))
                }

                val response = httpResponse.body<OAuthTokenResponse>()

                Log.d(TAG, "✅ Token exchange successful!")

                // Save tokens to DataStore
                TokenManager.saveOAuthTokens(
                    context = context,
                    accessToken = response.accessToken,
                    refreshToken = response.refreshToken,
                    tokenType = response.tokenType,
                    expiresIn = response.expiresIn
                )

                // ✅ Fetch civilId from userinfo endpoint and persist it
                fetchAndSaveCivilId(response.accessToken)

                Result.success(response)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Token exchange failed: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get current access token
     */
    suspend fun getAccessToken(): String? {
        return TokenManager.getAccessToken(context)
    }

    /**
     * Check if token is valid (not expired)
     */
    suspend fun isTokenValid(): Boolean {
        val token = TokenManager.getAccessToken(context)
        if (token.isNullOrEmpty()) return false
        return !TokenManager.isTokenExpired(context)
    }

    /**
     * Clear all tokens (logout)
     */
    suspend fun clearTokens() {
        TokenManager.clearToken(context)
    }

    /**
     * Refresh the access token using the stored refresh token
     */
    suspend fun refreshAccessToken(): Result<OAuthTokenResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🔄 Refreshing access token...")

                val refreshToken = TokenManager.getRefreshToken(context)
                if (refreshToken.isNullOrEmpty()) {
                    Log.e(TAG, "❌ No refresh token available")
                    return@withContext Result.failure(Exception("لا يوجد رمز تحديث متاح. يرجى تسجيل الدخول مرة أخرى"))
                }

                val httpResponse = httpClient.submitForm(
                    url = TOKEN_ENDPOINT,
                    formParameters = Parameters.build {
                        append("grant_type", "refresh_token")
                        append("refresh_token", refreshToken)
                        append("client_id", CLIENT_ID)
                    }
                )

                if (httpResponse.status.value != 200) {
                    val errorBody = try {
                        httpResponse.bodyAsText()
                    } catch (e: Exception) {
                        "Unknown error"
                    }

                    Log.e(TAG, "❌ Token refresh failed with status ${httpResponse.status.value}: $errorBody")

                    val errorMessage = when {
                        errorBody.contains("Token is not active") || errorBody.contains("invalid_grant") ->
                            "انتهت صلاحية رمز التحديث. يرجى تسجيل الدخول مرة أخرى"
                        else -> "فشل تحديث الرمز. يرجى تسجيل الدخول مرة أخرى"
                    }

                    return@withContext Result.failure(Exception(errorMessage))
                }

                val response = httpResponse.body<OAuthTokenResponse>()

                Log.d(TAG, "✅ Token refresh successful!")

                TokenManager.saveOAuthTokens(
                    context = context,
                    accessToken = response.accessToken,
                    refreshToken = response.refreshToken ?: refreshToken,
                    tokenType = response.tokenType,
                    expiresIn = response.expiresIn
                )

                // ✅ Re-fetch civilId from userinfo after token refresh
                fetchAndSaveCivilId(response.accessToken)

                Result.success(response)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Token refresh failed: ${e.message}", e)
                Result.failure(Exception("فشل تحديث الرمز. يرجى تسجيل الدخول مرة أخرى"))
            }
        }
    }

    /**
     * Calls the userinfo endpoint with the given access token and persists the civilId.
     */
    private suspend fun fetchAndSaveCivilId(accessToken: String) {
        try {
            Log.d(TAG, "🔄 Fetching userinfo to get civilId...")
            val userinfoResponse = httpClient.get(USERINFO_ENDPOINT) {
                header("Authorization", "Bearer $accessToken")
            }
            val bodyText = userinfoResponse.bodyAsText()
            Log.d(TAG, "📋 Userinfo response: $bodyText")
            val json = JSONObject(bodyText)
            val civilId = json.optString("civilId").takeIf { it.isNotEmpty() }
            if (civilId != null) {
                TokenManager.saveCivilId(context, civilId)
                Log.d(TAG, "✅ civilId from userinfo saved: $civilId")
            } else {
                Log.w(TAG, "⚠️ civilId not found in userinfo response")
            }
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Failed to fetch userinfo (civilId not saved): ${e.message}", e)
        }
    }
}
