package com.informatique.mtcit.data.dto

import kotlinx.serialization.Serializable

/**
 * Crew Member DTOs for Ship Navigation License Request
 * Used in both Issue and Renew transactions
 */

// ============================================
// REQUEST DTOs (Client → Server)
// ============================================

/**
 * Request DTO for adding/updating a crew member
 * @param nameAr Crew member name in Arabic (required)
 * @param nameEn Crew member name in English (required)
 * @param jobTitle Job title ID from lookup (required)
 * @param civilNo Civil number (optional)
 * @param seamenBookNo Seamen book number (required)
 * @param nationality Nationality information (optional)
 */
@Serializable
data class CrewReqDto(
    val nameAr: String,
    val nameEn: String,
    val jobTitle: Int,
    val civilNo: String? = null,
    val seamenBookNo: String,
    val nationality: CountryReqDto? = null
)

/**
 * Country/Nationality request wrapper
 * @param id Country ID from lookup
 */
@Serializable
data class CountryReqDto(
    val id: Int
)

// ============================================
// RESPONSE DTOs (Server → Client)
// ============================================

/**
 * Response DTO containing crew member details
 * @param id Database ID of the crew member
 * @param nameAr Name in Arabic
 * @param nameEn Name in English
 * @param jobTitle Job title information with bilingual names
 * @param civilNo Civil number
 * @param seamenBookNo Seamen book number
 * @param nationality Nationality information with bilingual names
 * @param shipNavigationRequestId Associated request ID
 * @param shipInfoId Associated ship info ID
 */
@Serializable
data class CrewResDto(
    val id: Long,
    val nameAr: String,
    val nameEn: String,
    val jobTitle: JobTitleResDto,
    val civilNo: String? = null,
    val seamenBookNo: String,
    val nationality: CountryResDto? = null,
    val shipNavigationRequestId: Long,
    val shipInfoId: Long
)

/**
 * Job title response with bilingual names
 */
@Serializable
data class JobTitleResDto(
    val id: Int,
    val nameAr: String,
    val nameEn: String
)

/**
 * Country/Nationality response with bilingual names
 */
@Serializable
data class CountryResDto(
    val id: Int,
    val nameAr: String,
    val nameEn: String
)

