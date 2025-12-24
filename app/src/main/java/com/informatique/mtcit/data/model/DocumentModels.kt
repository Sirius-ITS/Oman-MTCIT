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
