package com.informatique.mtcit.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Models for Mortgage Redemption (Release Mortgage) Request API
 */

/**
 * Request body for creating a new mortgage redemption request
 * POST /mortgage-redemption-request
 *
 * Example:
 * {
 *   "shipInfoId": 321,
 *   "documents": [
 *     {"fileName": "wallpaper.png", "documentId": 101}
 *   ]
 * }
 */
@Serializable
data class CreateRedemptionRequest(
    @SerialName("shipInfoId")
    val shipInfoId: Int,

    @SerialName("documents")
    val documents: List<RedemptionDocumentRef>? = null
)

/**
 * Document reference in redemption request
 */
@Serializable
data class RedemptionDocumentRef(
    @SerialName("fileName")
    val fileName: String,

    @SerialName("documentId")
    val documentId: Int
)

/**
 * Response from creating a redemption request
 */
@Serializable
data class CreateRedemptionResponse(
    val message: String,
    val statusCode: Int,
    val success: Boolean,
    val timestamp: String,
    val data: RedemptionRequestData
)

/**
 * Redemption request data in the response
 */
@Serializable
data class RedemptionRequestData(
    val id: Int,
    val ship: RedemptionShipRef? = null,
    val status: RedemptionStatusRef? = null,
    val documents: List<RedemptionDocumentData>? = null
)

/**
 * Ship reference in redemption response
 */
@Serializable
data class RedemptionShipRef(
    val id: Int,
    val changerDisplayName: String? = null,
    val createdByDisplayName: String? = null
)

/**
 * Status reference in redemption response
 */
@Serializable
data class RedemptionStatusRef(
    val id: Int,
    val changerDisplayName: String? = null,
    val createdByDisplayName: String? = null
)

/**
 * Document data in redemption response
 */
@Serializable
data class RedemptionDocumentData(
    val id: Int,
    val fileName: String,
    val documentId: Int
)

