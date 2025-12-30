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
}

