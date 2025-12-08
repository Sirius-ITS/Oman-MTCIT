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
