package com.informatique.mtcit.data.model.requests

import kotlinx.serialization.Serializable

/**
 * API Response Model for Status Count
 * Endpoint: GET /api/v1/registration-request-view/customer/count-by-status/{customerId}
 */

/**
 * Main response wrapper from the API
 */
@Serializable
data class StatusCountResponse(
    val statusCode: Int,
    val success: Boolean,
    val message: String? = null,
    val timestamp: String? = null,
    val data: StatusCountData? = null
)

/**
 * Data container with total count and status counts
 */
@Serializable
data class StatusCountData(
    val totalCount: Int,
    val statusCounts: List<StatusCount>
)

/**
 * Individual status count item
 */
@Serializable
data class StatusCount(
    val statusId: Int,
    val count: Int
)
