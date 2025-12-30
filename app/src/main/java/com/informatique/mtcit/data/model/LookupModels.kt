package com.informatique.mtcit.data.model

import kotlinx.serialization.Serializable

/**
 * Data models for lookup/dropdown values
 */

@Serializable
data class Port(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val code: String? = null
)

@Serializable
data class Country(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val isoCode: String,
    val capitalAr: String? = null,
    val capitalEn: String? = null
)

@Serializable
data class ShipType(
    val id: Int,
    val nameAr: String,
    val nameEn: String,
    val shipCategory: ShipCategoryRef? = null
)

@Serializable
data class ShipCategoryRef(
    val id: Int
)

@Serializable
data class ShipCategory(
    val id: Int,
    val nameAr: String,
    val nameEn: String
)

@Serializable
data class EngineStatus(
    val id: String,
    val nameAr: String,
    val nameEn: String
)

@Serializable
data class EngineType(
    val id: String,
    val nameAr: String,
    val nameEn: String
)

@Serializable
data class FuelType(
    val id: String,
    val nameAr: String,
    val nameEn: String
)

@Serializable
data class ProofType(
    val id: Int,
    val nameAr: String,
    val nameEn: String
)

@Serializable
data class MarineActivity(
    val id: Int,
    val nameAr: String,
    val nameEn: String
)

@Serializable
data class BuildMaterial(
    val id: Int,
    val nameAr: String,
    val nameEn: String
)

@Serializable
data class NavigationArea(
    val id: Int,
    val nameAr: String,
    val nameEn: String
)

@Serializable
data class MortgageReason(
    val id: Int,
    val nameAr: String,
    val nameEn: String
)

@Serializable
data class Bank(
    val id: String,
    val nameAr: String,
    val nameEn: String
)

@Serializable
data class CrewJobTitle(
    val id: Int,
    val nameAr: String,
    val nameEn: String
)

// ✅ NEW: Inspection-specific models
@Serializable
data class InspectionPurpose(
    val id: Int,
    val nameAr: String,
    val nameEn: String,
    val name: String
)

@Serializable
data class InspectionAuthority(
    val id: Int,
    val nameAr: String,
    val nameEn: String,
    val name: String,
    val type: InspectionAuthorityType
)

@Serializable
data class InspectionAuthorityType(
    val id: Int,
    val nameAr: String,
    val nameEn: String,
    val name: String
)

/**
 * Reference types for API requests
 * These are used when sending data to the API (only ID is needed)
 */
@Serializable
data class MarineActivityRef(
    val id: Int
)

@Serializable
data class ShipTypeRef(
    val id: Int,
    val shipCategory: ShipCategoryRef
)

@Serializable
data class ProofTypeRef(
    val id: Int
)

@Serializable
data class City(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val countryId: String
)

/**
 * Generic API response wrapper for lookups
 */
@Serializable
data class LookupResponse<T>(
    val success: Boolean,
    val data: List<T>,
    val message: String? = null
)

/**
 * Paginated API response for lookups (matches real API structure)
 */
@Serializable
data class PaginatedLookupResponse<T>(
    val message: String,
    val statusCode: Int,
    val success: Boolean,
    val timestamp: String,
    val data: PaginatedData<T>
)

@Serializable
data class PaginatedData<T>(
    val content: List<T>,
    val pageable: Pageable,
    val last: Boolean,
    val totalPages: Int,
    val totalElements: Int,
    val size: Int,
    val number: Int,
    val first: Boolean,
    val numberOfElements: Int,
    val empty: Boolean
)

@Serializable
data class Pageable(
    val pageNumber: Int,
    val pageSize: Int,
    val offset: Int,
    val unpaged: Boolean,
    val paged: Boolean
)

/**
 * Response from proceed-request API
 * POST /mortgage-request/ship-info/{shipInfoId}/proceed-request
 */
@Serializable
data class ProceedRequestResponse(
    val message: String,
    val statusCode: Int,
    val success: Boolean,
    val timestamp: String,
    val data: ProceedRequestData
)

/**
 * Data returned from proceed-request
 */
@Serializable
data class ProceedRequestData(
    val id: Int,
    val shipInfo: ShipInfoRef? = null,
    val requestSerial: Int? = null,
    val requestYear: Int? = null,
    val requestType: RequestTypeRef? = null,
    val status: StatusRef? = null
)

/**
 * Ship info reference in proceed-request response
 */
@Serializable
data class ShipInfoRef(
    val id: Int,
    val ship: ShipRef? = null,
    val isCurrent: Int? = null
)

/**
 * Ship reference in proceed-request response
 */
@Serializable
data class ShipRef(
    val id: Int,
    val shipName: String? = null,
    val imoNumber: Int? = null,
    val callSign: String? = null,
    val mmsiNumber: Int? = null  // ✅ Added mmsiNumber field
)

/**
 * Request type reference in proceed-request response
 */
@Serializable
data class RequestTypeRef(
    val id: Int,
    val nameAr: String? = null,
    val nameEn: String? = null
)

/**
 * Status reference in proceed-request response
 */
@Serializable
data class StatusRef(
    val id: Int,
    val nameAr: String? = null,
    val nameEn: String? = null
)
