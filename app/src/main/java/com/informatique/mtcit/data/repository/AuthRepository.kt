package com.informatique.mtcit.data.repository

import android.content.Context
import android.util.Log
import com.informatique.mtcit.data.datastorehelper.TokenManager
import com.informatique.mtcit.data.model.oauth.OAuthTokenResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
        private const val TOKEN_ENDPOINT = "https://omankeycloak.isfpegypt.com/realms/oman/protocol/openid-connect/token"
        private const val CLIENT_ID = "front"
        private const val REDIRECT_URI = "https://omankeycloak.isfpegypt.com/starter"
    }

    /**
     * Exchange authorization code for access token
     */
    suspend fun exchangeCodeForToken(authorizationCode: String): Result<OAuthTokenResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🔄 Exchanging authorization code for token...")

                val response = httpClient.submitForm(
                    url = TOKEN_ENDPOINT,
                    formParameters = Parameters.build {
                        append("grant_type", "authorization_code")
                        append("code", authorizationCode)
                        append("client_id", CLIENT_ID)
                        append("redirect_uri", REDIRECT_URI)
                    }
                ).body<OAuthTokenResponse>()

                Log.d(TAG, "✅ Token exchange successful!")

                // Save tokens to DataStore
                TokenManager.saveOAuthTokens(
                    context = context,
                    accessToken = response.accessToken,
                    refreshToken = response.refreshToken,
                    tokenType = response.tokenType,
                    expiresIn = response.expiresIn
                )

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
     * ✅ NEW: Added for 401 error handling
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

                // ✅ FIX: Get the full HTTP response to check status code
                val httpResponse = httpClient.submitForm(
                    url = TOKEN_ENDPOINT,
                    formParameters = Parameters.build {
                        append("grant_type", "refresh_token")
                        append("refresh_token", refreshToken)
                        append("client_id", CLIENT_ID)
                    }
                )

                // ✅ FIX: Check status code before deserializing
                if (httpResponse.status.value != 200) {
                    // Keycloak returned an error (400, 401, etc.)
                    val errorBody = try {
                        httpResponse.bodyAsText()
                    } catch (e: Exception) {
                        "Unknown error"
                    }

                    Log.e(TAG, "❌ Token refresh failed with status ${httpResponse.status.value}: $errorBody")

                    // Parse error message from Keycloak response
                    val errorMessage = when {
                        errorBody.contains("Token is not active") || errorBody.contains("invalid_grant") ->
                            "انتهت صلاحية رمز التحديث. يرجى تسجيل الدخول مرة أخرى"
                        else -> "فشل تحديث الرمز. يرجى تسجيل الدخول مرة أخرى"
                    }

                    return@withContext Result.failure(Exception(errorMessage))
                }

                // ✅ Success: Deserialize the response
                val response = httpResponse.body<OAuthTokenResponse>()

                Log.d(TAG, "✅ Token refresh successful!")

                // Save new tokens to DataStore
                TokenManager.saveOAuthTokens(
                    context = context,
                    accessToken = response.accessToken,
                    refreshToken = response.refreshToken ?: refreshToken, // Use new refresh token if provided, otherwise keep old one
                    tokenType = response.tokenType,
                    expiresIn = response.expiresIn
                )

                Result.success(response)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Token refresh failed: ${e.message}", e)
                Result.failure(Exception("فشل تحديث الرمز. يرجى تسجيل الدخول مرة أخرى"))
            }
        }
    }
}
