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
    val code: String
)

@Serializable
data class ShipType(
    val id: String,
    val nameAr: String,
    val nameEn: String
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

