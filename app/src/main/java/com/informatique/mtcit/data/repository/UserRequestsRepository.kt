package com.informatique.mtcit.data.repository

import com.informatique.mtcit.data.model.requests.RequestsApiResponse
import com.informatique.mtcit.data.model.requests.RequestDetailResponse

/**
 * Repository for User Requests
 * Handles data operations for requests with pagination support
 * Following app architecture pattern
 */
interface UserRequestsRepository {
    /**
     * Get user requests with pagination
     * @param civilId User's civil ID
     * @param size Number of items per page
     * @param page Page number (0-based)
     */
    suspend fun getUserRequests(
        civilId: String,
        size: Int = 10,
        page: Int = 0
    ): Result<RequestsApiResponse>

    /**
     * Get request detail by ID and type
     * @param requestId Request ID
     * @param endpointPath API endpoint path for this request type
     */
    suspend fun getRequestDetail(
        requestId: Int,
        endpointPath: String
    ): Result<RequestDetailResponse>

    /**
     * Refresh requests (clear cache if any)
     */
    suspend fun refreshRequests()
}
