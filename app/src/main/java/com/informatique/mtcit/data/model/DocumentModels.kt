package com.informatique.mtcit.data.model

import kotlinx.serialization.Serializable

/**
 * Document validation response models
 * Used for POST registration-requests/{requestId}/validate-build-status
 */

@Serializable
data class DocumentValidationResponse(
    val message: String,
    val statusCode: Int,
    val success: Boolean,
    val timestamp: String,
    val data: DocumentValidationData? = null,
    val errors: Map<String, String>? = null // Field-specific errors (e.g., "shipbuildingCertificate": "Required")
)

@Serializable
data class DocumentValidationData(
    val statusMessage: String? = null,
    val uploadedFiles: List<UploadedFile>? = null,
    val validated: Boolean? = null,
    val missingDocuments: List<String>? = null
)

@Serializable
data class UploadedFile(
    val fileName: String,
    val refNo: String,
    val contentType: String,
    val size: Int
)

/**
 * Document metadata for dynamic document uploads
 * Used in the DTO when uploading multiple documents
 */
@Serializable
data class DocumentMetadata(
    val fileName: String,
    val documentId: Int
)

/**
 * DTO wrapper for document validation with dynamic documents
 */
@Serializable
data class DocumentValidationRequestDto(
    val documents: List<DocumentMetadata>
)

/**
 * File upload wrapper for dynamic documents
 */
data class DocumentFileUpload(
    val fileName: String,
    val fileUri: String,
    val fileBytes: ByteArray,
    val mimeType: String,
    val documentId: Int
)

/**
 * Insurance Document Request DTO
 * Used for POST /api/v1/perm-registration-requests/validate-insurance-document
 * Note: File is sent separately in multipart, not in DTO
 */
@Serializable
data class InsuranceDocumentRequestDto(
    val shipInfoId: Int,
    val insuranceNumber: String,
    val countryId: String,
    val insuranceCompanyId: Int? = null, // Required only for Oman (countryId = "OM")
    val insuranceCompanyName: String? = null, // Used when countryId != "OM"
    val insuranceExpiryDate: String, // Format: "2025-12-12"
    val crNumber: String? = null // Optional - only for companies (not individuals)
)
/**
 * Insurance Document Response
 */
@Serializable
data class InsuranceDocumentResponse(
    val message: String,
    val statusCode: Int,
    val success: Boolean,
    val timestamp: String,
    val data: InsuranceDocumentData? = null
)

@Serializable
data class InsuranceDocumentData(
    val id: Int? = null,
    val shipInfo: InsuranceShipInfo? = null,
    val requestSerial: Int? = null,
    val requestYear: Int? = null,
    val requestType: IdWrapper? = null,
    val status: IdWrapper? = null, // ✅ Changed from String to IdWrapper (object with id)
    val documents: List<InsuranceDocument>? = null,
    val insuranceDoc: InsuranceDoc? = null,
    val insuranceInfo: List<String>? = null // Can be adjusted based on actual data
)

@Serializable
data class IdWrapper(
    val id: Int
)

@Serializable
data class InsuranceShipInfo(
    val id: Int,
    val ship: InsuranceShip? = null,
    val isCurrent: Int? = null
)

@Serializable
data class InsuranceShip(
    val id: Int,
    val shipName: String? = null,
    val registrationNumber: String? = null
)

@Serializable
data class InsuranceDocument(
    val id: Int? = null,
    val fileName: String? = null,
    val docRefNum: String? = null
)

@Serializable
data class InsuranceDoc(
    val docRefNum: String,
    val fileName: String,
    val docId: Int
)


