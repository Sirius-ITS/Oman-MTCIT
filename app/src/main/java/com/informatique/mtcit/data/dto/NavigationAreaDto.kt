package com.informatique.mtcit.data.dto

import kotlinx.serialization.Serializable

/**
 * Navigation Area DTOs for Ship Navigation License Request
 * Used in both Issue and Renew transactions
 */

// ============================================
// REQUEST DTOs (Client → Server)
// ============================================

/**
 * Request DTO for adding/updating a navigation area
 * @param areaId Navigation area ID from lookup/master data
 * @param attachmentFile File path/URL for the area permit document
 */
@Serializable
data class NavigationAreaReqDto(
    val areaId: Int,
    val attachmentFile: String? = null
)

// ============================================
// RESPONSE DTOs (Server → Client)
// ============================================

/**
 * Response DTO containing navigation area details
 * @param id Database ID of the navigation area record
 * @param areaId Area lookup ID
 * @param areaNameAr Area name in Arabic
 * @param areaNameEn Area name in English
 * @param attachmentFile URL/path to the uploaded permit document
 * @param shipNavigationRequestId Associated request ID
 */
@Serializable
data class NavigationAreaResDto(
    val id: Long,
    val areaId: Int,
    val areaNameAr: String,
    val areaNameEn: String,
    val attachmentFile: String? = null,
    val shipNavigationRequestId: Long
)

