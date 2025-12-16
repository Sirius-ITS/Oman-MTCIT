package com.informatique.mtcit.data.model

import kotlinx.serialization.Serializable

/**
 * Response model for fetching required documents by request type
 * GET api/v1/reqtype/{requestTypeId}/documents
 *
 * ✅ UPDATED: API returns documents directly in data array, not nested
 * Example response:
 * {
 *   "message": "Retrieved Successfully",
 *   "statusCode": 200,
 *   "success": true,
 *   "timestamp": "2025-12-16 15:32:50",
 *   "data": [
 *     {
 *       "id": 101,
 *       "nameAr": " رخصه سفينه",
 *       "nameEn": "ship license",
 *       "docOrder": 1,
 *       "isMandatory": 1,
 *       "isActive": 1
 *     }
 *   ]
 * }
 */
@Serializable
data class RequiredDocumentsResponse(
    val message: String,
    val statusCode: Int,
    val success: Boolean,
    val timestamp: String,
    val data: List<DocumentInfo> // ✅ Changed from RequiredDocumentItem to DocumentInfo
)

/**
 * Document information - returned directly in data array
 */
@Serializable
data class DocumentInfo(
    val id: Int,
    val nameAr: String,
    val nameEn: String? = null,
    val docOrder: Int? = 0,
    val isMandatory: Int = 0,
    val isActive: Int = 1
)

/**
 * Wrapper for documents (used internally to maintain compatibility)
 * ✅ This is created programmatically from DocumentInfo
 */
data class RequiredDocumentItem(
    val id: Int,
    val requestTypeId: Int? = null,
    val document: DocumentInfo
)


