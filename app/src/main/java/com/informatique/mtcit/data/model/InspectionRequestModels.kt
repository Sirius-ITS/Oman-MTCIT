package com.informatique.mtcit.data.model

import kotlinx.serialization.Serializable

/**
 * DTO for creating an inspection request with documents
 * POST /api/v1/inspection-requests
 * Content-Type: multipart/form-data
 *
 * API Expected Structure:
 * {
 *   "id": null | 0,          // null when adding new ship, or actual ID from proceed-request
 *   "shipInfoId": 0,
 *   "purposeId": 0,
 *   "authorityId": 0,
 *   "portId": "string",
 *   "crNumber": "string",
 *   "documents": [
 *     {
 *       "fileName": "string",
 *       "documentId": 0
 *     }
 *   ]
 * }
 */
@Serializable
data class CreateInspectionRequestDto(
    val id: Int? = null,                // Request ID: null for new ship, or actual ID from proceed-request/create request
    val shipInfoId: Int = 0,            // Ship info ID
    val purposeId: Int = 0,             // Inspection purpose ID
    val authorityId: Int = 0,           // Inspection authority ID
    val portId: String = "",            // Recording port ID (as string)
    val crNumber: String = "",          // Commercial registration number (empty string for individual)
    val documents: List<InspectionDocumentDto> = emptyList()
)

/**
 * Document metadata within the DTO
 * The actual file will be sent as multipart file with name "files"
 */
@Serializable
data class InspectionDocumentDto(
    val fileName: String,               // File name (e.g., "certificate.pdf")
    val documentId: Int                 // Document type ID from required documents API
)

/**
 * File upload item for inspection request
 */
data class InspectionFileUpload(
    val documentId: Int,
    val fileName: String,
    val fileBytes: ByteArray,
    val mimeType: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as InspectionFileUpload

        if (documentId != other.documentId) return false
        if (fileName != other.fileName) return false
        if (!fileBytes.contentEquals(other.fileBytes)) return false
        if (mimeType != other.mimeType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = documentId
        result = 31 * result + fileName.hashCode()
        result = 31 * result + fileBytes.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        return result
    }
}

/**
 * Response from creating inspection request
 */
@Serializable
data class CreateInspectionRequestResponse(
    val statusCode: Int,
    val success: Boolean,
    val message: String,
    val data: InspectionRequestData
)

@Serializable
data class InspectionRequestData(
    val id: Int = 1234,
    val requestSerial: String? = null,
    val requestYear: Int? = null,
    val inspectionPurpose: String? = null,
    val inspectionRecordingPort: String? = null,
    val inspectionAuthority: String? = null,
    val inspectionApprovedEntity: String? = null
)

