package com.informatique.mtcit.data.model

import kotlinx.serialization.Serializable

/**
 * Request model for creating a ship registration request
 * Used in: Temporary Registration, Permanent Registration, etc.
 */
@Serializable
data class CreateRegistrationRequest(
    val regShipRegRequestReqDto: RegShipRegRequestReqDto,
    val isCompany: Int
)

@Serializable
data class RegShipRegRequestReqDto(
    val id: Int? = null, // ✅ NEW: Request ID for PUT updates (null for initial POST)
    val shipInfo: ShipInfo,
    val requestType: RequestType
)

@Serializable
data class ShipInfo(
    val ship: Ship,
    val isCurrent: Int = 0 // Always 0 for now
)

@Serializable
data class Ship(
    val shipName: String,
    val imoNumber: Int? = null,
    val callSign: String,
    val mmsiNumber: Int? = null,
    val officialNumber: String? = null,
    val portOfRegistry: PortOfRegistryRef,
    val marineActivity: MarineActivityRef,
    val shipCategory: ShipCategoryRef,
    val shipType: ShipTypeRef,
    val proofType: ProofTypeRef,
    val buildCountry: BuildCountryRef? = null,
    val buildMaterial: BuildMaterialRef? = null,
    val shipBuildYear: Int,
    val buildEndDate: String,
    val shipYardName: String? = null,
    val firstRegistrationDate: String? = null,
    val requestSubmissionDate: String
)

@Serializable
data class PortOfRegistryRef(
    val id: String
)

@Serializable
data class BuildCountryRef(
    val id: String
)

@Serializable
data class BuildMaterialRef(
    val id: Int
)

@Serializable
data class RequestType(
    val id: Int
)

/**
 * Response model for created registration request
 */
@Serializable
data class CreateRegistrationResponse(
    val message: String,
    val statusCode: Int,
    val success: Boolean,
    val timestamp: String,
    val data: RegistrationRequestData
)

@Serializable
data class RegistrationRequestData(
    val id: Int, // The main request ID we need for PUT requests
    val shipInfo: ShipInfoResponse? = null,
    val requestSerial: Int? = null,
    val requestYear: Int? = null,
    val requestType: RequestTypeResponse? = null,
    val status: StatusResponse? = null
)

@Serializable
data class ShipInfoResponse(
    val id: Int,
    val ship: ShipResponse? = null,
    val isCurrent: Int? = null
)

@Serializable
data class ShipResponse(
    val id: Int,
    val shipName: String? = null,
    val imoNumber: Int? = null,
    val callSign: String? = null,
    val mmsiNumber: Int? = null,
    val officialNumber: String? = null,
    val portOfRegistry: PortResponse? = null,
    val marineActivity: MarineActivityResponse? = null,
    val shipCategory: ShipCategoryResponse? = null,
    val shipType: ShipTypeResponse? = null,
    val proofType: ProofTypeResponse? = null,
    val buildCountry: CountryResponse? = null,
    val buildMaterial: BuildMaterialResponse? = null,
    val shipBuildYear: Int? = null,
    val buildEndDate: String? = null,
    val shipYardName: String? = null,
    val firstRegistrationDate: String? = null,
    val requestSubmissionDate: String? = null,
    val isTemp: Int? = null
)

@Serializable
data class PortResponse(
    val id: String,
    val nameAr: String? = null,
    val nameEn: String? = null,
    val country: CountryResponse? = null
)

@Serializable
data class CountryResponse(
    val id: String,
    val nameAr: String? = null,
    val nameEn: String? = null,
    val isoCode: String? = null,
    val capitalAr: String? = null,
    val capitalEn: String? = null
)

@Serializable
data class MarineActivityResponse(
    val id: Int,
    val nameAr: String? = null,
    val nameEn: String? = null
)

@Serializable
data class ShipCategoryResponse(
    val id: Int,
    val nameAr: String? = null,
    val nameEn: String? = null
)

@Serializable
data class ShipTypeResponse(
    val id: Int,
    val nameAr: String? = null,
    val nameEn: String? = null,
    val shipCategory: ShipCategoryRef? = null
)

@Serializable
data class ProofTypeResponse(
    val id: Int,
    val nameAr: String? = null,
    val nameEn: String? = null
)

@Serializable
data class BuildMaterialResponse(
    val id: Int,
    val nameAr: String? = null,
    val nameEn: String? = null
)

@Serializable
data class RequestTypeResponse(
    val id: Int
)

@Serializable
data class StatusResponse(
    val id: Int,
    val nameAr: String? = null,
    val nameEn: String? = null
)

/**
 * Response model for send-request API
 * POST registration-requests/{request-id}/send-request
 */
@Serializable
data class SendRequestResponse(
    val message: String,
    val statusCode: Int,
    val success: Boolean,
    val timestamp: String,
    val data: SendRequestData
)

@Serializable
data class SendRequestData(
    val message: String,
    val needInspection: Boolean
)

/**
 * Models for Navigation License creation response
 */
@Serializable
data class CreateNavigationResponse(
    val message: String,
    val statusCode: Int,
    val success: Boolean,
    val timestamp: String? = null,
    val data: NavigationRequestData? = null
)

@Serializable
data class NavigationRequestData(
    val id: Int,
    val shipInfo: ShipInfoResponse? = null,
    val requestSerial: Int? = null,
    val requestYear: Int? = null,
    val requestType: RequestTypeResponse? = null,
    val status: StatusResponse? = null
)
