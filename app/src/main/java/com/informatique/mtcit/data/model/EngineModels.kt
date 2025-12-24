package com.informatique.mtcit.data.model

import kotlinx.serialization.Serializable

/**
 * Engine submission request models
 * Used for POST registration-requests/{requestId}/engines
 */

@Serializable
data class EngineSubmissionRequest(
    val engineSerialNumber: String,
    val engineType: EngineTypeRef,
    val enginePower: Double,
    val cylindersCount: Int,
    val engineModel: String,
    val engineManufacturer: String,
    val engineCountry: EngineCountryRef? = null,
    val engineBuildYear: String,
    val engineFuelType: EngineFuelTypeRef? = null,
    val engineStatus: EngineStatusRef,
    val docOwnerId: String,
    val documents: List<EngineDocumentMetadata>
)

@Serializable
data class EngineTypeRef(
    val id: String,
    val nameEn: String,
    val nameAr: String
)

@Serializable
data class EngineCountryRef(
    val id: String,
    val nameEn: String,
    val nameAr: String
)

@Serializable
data class EngineFuelTypeRef(
    val id: String,
    val nameEn: String,
    val nameAr: String
)

@Serializable
data class EngineStatusRef(
    val id: String,
    val nameEn: String,
    val nameAr: String
)

@Serializable
data class EngineDocumentMetadata(
    val fileName: String,
    val docOwnerId: String,
    val docId: Int = 1 // Document type ID (default to 1 for backward compatibility)
)

/**
 * Engine submission response
 */
@Serializable
data class EngineSubmissionResponse(
    val message: String,
    val statusCode: Int,
    val success: Boolean,
    val timestamp: String,
    val data: List<EngineResponseData>? = null
)

@Serializable
data class EngineResponseData(
    val id: Int,
    val engineSerialNumber: String? = null,
    val engineType: EngineTypeRef? = null,
    val enginePower: Double? = null,
    val cylindersCount: Int? = null,
    val engineModel: String? = null,
    val engineManufacturer: String? = null,
    val engineCountry: EngineCountryRef? = null,
    val engineBuildYear: String? = null,
    val engineFuelType: EngineFuelTypeRef? = null,
    val engineStatus: EngineStatusRef? = null,
    val docOwnerId: String? = null
)

/**
 * Data class to hold file information for upload
 */
data class EngineFileUpload(
    val fileName: String,
    val fileUri: String,
    val fileBytes: ByteArray,
    val mimeType: String = "application/octet-stream",
    val docOwnerId: String? = null, // Which engine this file belongs to
    val docId: Int = 1 // Which document type (default to 1)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EngineFileUpload

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
        result = 31 * result + (docOwnerId?.hashCode() ?: 0)
        result = 31 * result + docId
        return result
    }
}
