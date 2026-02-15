package com.informatique.mtcit.data.model.requests

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Generic API Response wrapper for request details
 * Supports dynamic data structure for different request types
 */
@Serializable
data class RequestDetailResponse(
    val message: String,
    val statusCode: Int,
    val success: Boolean,
    val timestamp: String,
    val data: JsonElement // Dynamic data that varies per request type
)

/**
 * Parsed request detail data for UI display
 * This is created by parsing the JsonElement dynamically
 */
data class RequestDetailUiModel(
    val requestId: Int,
    val requestSerial: String,
    val requestYear: Int,
    val requestType: RequestTypeInfo,
    val status: RequestStatusInfo,
    val message: String?,
    val messageDetails: String?,
    val sections: List<RequestDetailSection>,
    val isPaid: Int = 0, // 0 = not paid, 1 = paid
    val hasAcceptance: Int = 0, // 0 = continue to payment, 1 = requires acceptance
    val shipName: String? = null, // ✅ Ship name for display in header card
    val purposeId: Int? = null, // ✅ Purpose ID for checklist loading (engineer only)
    val workOrderResult: com.informatique.mtcit.data.model.WorkOrderResult? = null, // ✅ Completed checklist answers (engineer only)
    val scheduledRequestId: Int? = null // ✅ Scheduled request ID (root data.id for scheduled inspections)
)

/**
 * A section in the request detail (e.g., "Ship Info", "Owners", "Engines")
 */
data class RequestDetailSection(
    val title: String,
    val fields: List<RequestDetailField>
)

/**
 * A field in a section (key-value pair or nested object)
 */
sealed class RequestDetailField {
    data class SimpleField(
        val label: String,
        val value: String
    ) : RequestDetailField()

    data class NestedObject(
        val label: String,
        val fields: List<RequestDetailField>
    ) : RequestDetailField()

    data class ArrayField(
        val label: String,
        val items: List<List<RequestDetailField>>
    ) : RequestDetailField()
}

/**
 * Request type information
 */
data class RequestTypeInfo(
    val id: Int,
    val name: String,
    val nameAr: String? = null,
    val nameEn: String? = null
)

/**
 * Request status information
 */
data class RequestStatusInfo(
    val id: Int,
    val name: String,
    val nameAr: String? = null,
    val nameEn: String? = null
)

