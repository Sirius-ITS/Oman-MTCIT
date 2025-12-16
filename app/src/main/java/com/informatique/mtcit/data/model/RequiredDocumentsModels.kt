package com.informatique.mtcit.data.model

import kotlinx.serialization.Serializable

/**
 * Response model for fetching required documents by request type
 * GET api/v1/reqtype/{requestTypeId}/documents
 */
@Serializable
data class RequiredDocumentsResponse(
    val message: String,
    val statusCode: Int,
    val success: Boolean,
    val timestamp: String,
    val data: List<RequiredDocumentItem>
)

/**
 * Individual required document item
 */
@Serializable
data class RequiredDocumentItem(
    val id: Int,
    val requestTypeId: Int,
    val document: DocumentInfo
)

/**
 * Document information
 */
@Serializable
data class DocumentInfo(
    val id: Int,
    val nameAr: String,
    val nameEn: String,
    val docOrder: Int,
    val isMandatory: Int, // 1 = mandatory, 0 = optional
    val isActive: Int // 1 = active, 0 = inactive
)

