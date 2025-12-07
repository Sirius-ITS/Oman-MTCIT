package com.informatique.mtcit.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Models for Mortgage Request API
 */

/**
 * Request body for creating a new mortgage request
 * POST /api/v1/mortgage-request
 */
@Serializable
data class CreateMortgageRequest(
    // server now expects `shipInfoId` in the JSON
    @SerialName("shipInfoId")
    val shipInfoId: Int = 26,
    @SerialName("bankId")
    val bankId: Int = 9,
    @SerialName("mortgageReasonId")
    val mortgageReasonId: Int = 1,
    @SerialName("financingContractNumber")
    val financingContractNumber: String = "FFSEF89",
    @SerialName("startDate")
    val startDate: String = "2025-11-20",
    // backend expects `mortgageValue` in the request body
    @SerialName("mortgageValue")
    val mortgageValue: Double = 5.1,
    @SerialName("statusId")
    val statusId: Int = 1  // Always 1 for new mortgage requests
)

/**
 * Response from creating a mortgage request
 */
@Serializable
data class CreateMortgageResponse(
    val message: String,
    val statusCode: Int,
    val success: Boolean,
    val timestamp: String,
    val data: MortgageRequestData
)

/**
 * Mortgage request data in the response
 */
@Serializable
data class MortgageRequestData(
    val id: Int,
    val ship: MortgageShipRef,
    val bank: MortgageBankRef,
    @SerialName("value")
    val mortgageValue: Double ,
    val startDate: String,
    val financingContractNumber: String,
    val mortgageReason: MortgageReasonRef,
    val status: MortgageStatusRef
)

/**
 * Ship reference in mortgage response
 */
@Serializable
data class MortgageShipRef(
    val changerDisplayName: String? = null,
    val createdByDisplayName: String? = null,
    val id: Int,
    val build: Boolean? = null
)

/**
 * Bank reference in mortgage response
 */
@Serializable
data class MortgageBankRef(
    val changerDisplayName: String? = null,
    val createdByDisplayName: String? = null,
    val id: Int
)

/**
 * Mortgage reason reference in mortgage response
 */
@Serializable
data class MortgageReasonRef(
    val changerDisplayName: String? = null,
    val createdByDisplayName: String? = null,
    val id: Int
)

/**
 * Status reference in mortgage response
 */
@Serializable
data class MortgageStatusRef(
    val changerDisplayName: String? = null,
    val createdByDisplayName: String? = null,
    val id: Int
)

/**
 * Request body for creating a mortgage redemption (release) request
 * POST /api/v1/mortgage-redemption-request
 */
@Serializable
data class CreateMortgageRedemptionRequest(
    @SerialName("shipInfoId")
    val shipInfoId: Int,
    @SerialName("statusId")
    val statusId: Int = 1  // Always 1 for new redemption requests
)

/**
 * Response from creating a mortgage redemption request
 */
@Serializable
data class CreateMortgageRedemptionResponse(
    val message: String,
    val statusCode: Int,
    val success: Boolean,
    val timestamp: String,
    val data: MortgageRedemptionData
)

/**
 * Mortgage redemption data in the response
 * Fields are optional because backend structure may vary
 */
@Serializable
data class MortgageRedemptionData(
    val id: Int,
    val ship: MortgageShipRef? = null,
    val status: MortgageStatusRef? = null,
    // Additional fields that might come from backend
    val shipInfoId: Int? = null,
    val statusId: Int? = null
)

