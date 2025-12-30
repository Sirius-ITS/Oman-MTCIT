package com.informatique.mtcit.data.model.oauth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * OAuth Token Response from Keycloak
 */
@Serializable
data class OAuthTokenResponse(
    @SerialName("access_token")
    val accessToken: String,

    @SerialName("refresh_token")
    val refreshToken: String? = null,

    @SerialName("token_type")
    val tokenType: String = "Bearer",

    @SerialName("expires_in")
    val expiresIn: Long? = null,

    @SerialName("refresh_expires_in")
    val refreshExpiresIn: Long? = null,

    @SerialName("scope")
    val scope: String? = null
)

/**
 * OAuth Error Response
 */
@Serializable
data class OAuthErrorResponse(
    @SerialName("error")
    val error: String,

    @SerialName("error_description")
    val errorDescription: String? = null
)

