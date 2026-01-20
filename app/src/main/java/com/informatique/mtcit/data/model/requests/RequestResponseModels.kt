package com.informatique.mtcit.data.model.requests

import com.informatique.mtcit.business.transactions.TransactionType
import kotlinx.serialization.Serializable

/**
 * API Response Models for User Requests
 * Endpoint: GET /request/{civilId}/user-requests?size={size}&page={page}
 */

/**
 * Main response wrapper from the API
 */
@Serializable
data class RequestsApiResponse(
    val statusCode: Int,
    val success: Boolean,
    val message: String? = null,
    val data: RequestsData? = null
)

/**
 * Data container with requests list and pagination info
 */
@Serializable
data class RequestsData(
    val content: List<RequestItem>,
    val pageable: PageableInfo,
    val totalPages: Int,
    val totalElements: Int,
    val last: Boolean,
    val size: Int,
    val number: Int,
    val sort: List<SortInfo> = emptyList(), // Add sort field here as well
    val numberOfElements: Int,
    val first: Boolean,
    val empty: Boolean
)

/**
 * Individual request item from the API
 */
@Serializable
data class RequestItem(
    val id: Int,
    val requestId: Int? = null,
    val requestSerial: String? = null, // e.g., "1252/2025"
    val requestNumber: String? = null, // ✅ NEW: Engineer API uses "requestNumber" instead of "requestSerial"
    val creationDate: String? = null,
    val modificationDate: String? = null,
    val lastChange: String? = null, // e.g., "2025-12-29T10:16:55.008"
    val createdAt: String? = null,
    val modifiedBy: String? = null,
    val createdBy: String? = null,
    val requestTypeId: Int? = null,
    val requestTypeName: String? = null,
    val shipId: Int? = null,
    val shipName: String? = null,
    val shipNumber: String? = null,
    val shipTypeId: Int? = null,
    val statusId: Int? = null,
    val statusName: String? = null,
    val rejectionReason: String? = null,
    val ownerId: String? = null,
    val ownerName: String? = null,
    val isFinal: String? = null,
    // ✅ NEW: Support for nested objects from engineer API (correct structure)
    val requestStatus: StatusObject? = null, // Engineer API uses "requestStatus" not "status"
    val requestType: RequestTypeObject? = null,
    val ship: ShipObject? = null,
    val shipInfo: ShipInfoObject? = null // ✅ NEW: Engineer API has shipInfo wrapper
) {
    /**
     * Get the TransactionType from requestTypeId or requestType object
     */
    fun getTransactionType(): TransactionType? {
        val typeId = requestTypeId ?: requestType?.id
        return typeId?.let { TransactionType.fromTypeId(it) }
    }

    /**
     * Get the display name for this request type
     */
    fun getRequestTypeDisplayName(): String {
        // For engineer API, all requests are inspection requests
        return requestTypeName
            ?: requestType?.nameAr
            ?: requestTypeId?.let { TransactionType.getDisplayName(it) }
            ?: "طلب معاينة" // Default for engineer inspection requests
    }

    /**
     * Get the actual status ID from either statusId or requestStatus object
     */
    fun getActualStatusId(): Int {
        return statusId ?: requestStatus?.id ?: 1
    }

    /**
     * Get the actual status name from either statusName or requestStatus object
     */
    fun getActualStatusName(): String? {
        return statusName ?: requestStatus?.nameAr
    }

    /**
     * Get the actual ship name from multiple possible locations
     */
    fun getActualShipName(): String? {
        return shipName
            ?: ship?.shipName
            ?: shipInfo?.ship?.shipName // ✅ Check nested shipInfo.ship.shipName
    }

    /**
     * Get request serial/number for display (engineer API uses "requestNumber")
     */
    fun getDisplayRequestNumber(): String {
        return requestNumber ?: requestSerial ?: "#$id"
    }
}

/**
 * Status object that might come from engineer API (as "requestStatus")
 */
@Serializable
data class StatusObject(
    val id: Int,
    val nameAr: String? = null,
    val nameEn: String? = null,
    val name: String? = null
)

/**
 * Request type object that might come from engineer API
 */
@Serializable
data class RequestTypeObject(
    val id: Int,
    val nameAr: String? = null,
    val nameEn: String? = null
)

/**
 * Ship object that might come from engineer API
 */
@Serializable
data class ShipObject(
    val id: Int? = null,
    val shipName: String? = null,
    val imoNumber: Int? = null,
    val callSign: String? = null
)

/**
 * ShipInfo wrapper object from engineer API
 */
@Serializable
data class ShipInfoObject(
    val id: Int? = null,
    val ship: ShipObject? = null,
    val isCurrent: Int? = null
)

/**
 * Pagination information
 */
@Serializable
data class PageableInfo(
    val pageNumber: Int,
    val pageSize: Int,
    val sort: List<SortInfo> = emptyList(), // Changed from SortInfo to List<SortInfo> with default empty list
    val offset: Int,
    val paged: Boolean,
    val unpaged: Boolean
)

/**
 * Sort information
 */
@Serializable
data class SortInfo(
    val empty: Boolean? = null,
    val sorted: Boolean? = null,
    val unsorted: Boolean? = null
)

/**
 * UI Model for displaying requests (mapped from RequestItem)
 */
data class UserRequestUiModel(
    val id: Int,
    val requestSerial: String, // e.g., "1252/2025"
    val requestTypeId: Int,
    val requestTypeName: String,
    val shipId: Int?,
    val shipName: String,
    val shipNumber: String,
    val statusId: Int,
    val statusName: String,
    val statusColor: androidx.compose.ui.graphics.Color,
    val statusBgColor: androidx.compose.ui.graphics.Color,
    val creationDate: String,
    val modificationDate: String,
    val rejectionReason: String?
)

/**
 * Pagination state for UI
 */
data class PaginationState(
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val totalElements: Int = 0,
    val isLastPage: Boolean = false,
    val isFirstPage: Boolean = true,
    val hasMore: Boolean = false
)
