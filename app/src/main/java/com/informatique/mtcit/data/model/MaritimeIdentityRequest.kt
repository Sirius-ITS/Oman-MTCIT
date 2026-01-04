package com.informatique.mtcit.data.model

import kotlinx.serialization.Serializable

/**
 * Request body for adding maritime identification to a ship
 * PATCH /api/v1/perm-registration-requests/{shipId}/add-ship-identity
 *
 * This endpoint is used to add IMO, MMSI, and Call Sign for a ship
 * only when these fields are missing. If the ship already has all three
 * fields, this screen should not appear.
 */
@Serializable
data class MaritimeIdentityRequest(
    val imoNumber: String? = null,
    val mmsiNumber: String? = null,
    val callSign: String? = null
)

/**
 * Response for maritime identity update
 */
@Serializable
data class MaritimeIdentityResponse(
    val success: Boolean = false,
    val statusCode: Int = 0,
    val message: String = "",
    val data: MaritimeIdentityData? = null
)

@Serializable
data class MaritimeIdentityData(
    val id: Int? = null,
    val imoNumber: String? = null,
    val mmsiNumber: String? = null,
    val callSign: String? = null
)

