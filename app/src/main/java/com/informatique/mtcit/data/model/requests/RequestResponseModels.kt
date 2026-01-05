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
    val creationDate: String? = null,
    val modificationDate: String? = null,
    val lastChange: String? = null, // e.g., "2025-12-29T10:16:55.008"
    val createdAt: String? = null,
    val modifiedBy: String? = null,
    val createdBy: String? = null,
    val requestTypeId: Int,
    val requestTypeName: String? = null,
    val shipId: Int? = null,
    val shipName: String? = null,
    val shipNumber: String? = null,
    val shipTypeId: Int? = null,
    val statusId: Int,
    val statusName: String? = null,
    val rejectionReason: String? = null,
    val ownerId: String? = null,
    val ownerName: String? = null,
    val isFinal: String? = null
) {
    /**
     * Get the TransactionType from requestTypeId
     */
    fun getTransactionType(): TransactionType? {
        return TransactionType.fromTypeId(requestTypeId)
    }

    /**
     * Get the display name for this request type
     */
    fun getRequestTypeDisplayName(): String {
        return TransactionType.getDisplayName(requestTypeId)
    }
}

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
