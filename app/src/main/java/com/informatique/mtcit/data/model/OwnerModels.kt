package com.informatique.mtcit.data.model

import kotlinx.serialization.Serializable

/**
 * Owner submission request models
 * Used for POST registration-requests/{requestId}/owners
 */

@Serializable
data class OwnerSubmissionRequest(
    val isCompany: Int, // 0 = Individual, 1 = Company
    val ownerName: String, // Arabic name
    val ownerNameEn: String? = null, // English name
    val ownerCivilId: String? = null, // For individuals
    val commercialRegNumber: String? = null, // For companies
    val ownershipPercentage: Double,
    val isRepresentative: Int, // 0 = No, 1 = Yes
    val ownerAddress: String? = null,
    val ownerPhone: String? = null,
    val ownerEmail: String? = null,
    val docOwnerId: String,
    val documents: List<OwnerDocumentMetadata>
)

@Serializable
data class OwnerDocumentMetadata(
    val fileName: String,
    val docOwnerId: String,
    val docId: Int // Document type ID (1, 2, 3, etc.)
)

/**
 * Owner submission response
 */
@Serializable
data class OwnerSubmissionResponse(
    val message: String,
    val statusCode: Int,
    val success: Boolean,
    val timestamp: String,
    val data: List<OwnerResponseData>? = null
)

@Serializable
data class OwnerResponseData(
    val id: Int,
    val isCompany: Int? = null,
    val ownerName: String? = null,
    val ownerCivilId: String? = null,
    val commercialRegNumber: String? = null,
    val ownershipPercentage: Double? = null,
    val isRepresentative: Int? = null,
    val ownerAddress: String? = null,
    val ownerPhone: String? = null,
    val ownerEmail: String? = null,
    val docOwnerId: String? = null
)

/**
 * Data class to hold owner file information for upload
 */
data class OwnerFileUpload(
    val fileName: String,
    val fileUri: String,
    val fileBytes: ByteArray,
    val mimeType: String = "",
    val docOwnerId: String, // Which owner this file belongs to
    val docId: Int // Which document type (1, 2, 3, etc.)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OwnerFileUpload

        if (fileName != other.fileName) return false
        if (fileUri != other.fileUri) return false
        if (!fileBytes.contentEquals(other.fileBytes)) return false
        if (mimeType != other.mimeType) return false
        if (docOwnerId != other.docOwnerId) return false
        if (docId != other.docId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + fileUri.hashCode()
        result = 31 * result + fileBytes.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + docOwnerId.hashCode()
        result = 31 * result + docId
        return result
    }
}
