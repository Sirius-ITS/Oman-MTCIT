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
     * @param sort Sort order (e.g., "lastChange,desc" or "lastChange,asc")
     */
    suspend fun getUserRequests(
        civilId: String,
        size: Int = 10,
        page: Int = 0,
        sort: String = "lastChange,desc"
    ): Result<RequestsApiResponse>

    /**
     * Get filtered user requests with pagination
     * Uses Base64 encoded filter parameter
     * @param civilId User's civil ID
     * @param filter RequestFilterDto containing filter criteria
     */
    suspend fun getFilteredUserRequests(
        civilId: String,
        filter: com.informatique.mtcit.data.model.requests.RequestFilterDto
    ): Result<RequestsApiResponse>

    /**
     * Get engineer inspection requests with pagination
     * Uses Base64 encoded filter parameter with simplified structure
     * @param page Page number (0-based)
     * @param size Number of items per page
     * @param searchText Optional search text
     * @param columnName Column to search in
     */
    suspend fun getEngineerInspectionRequests(
        page: Int = 0,
        size: Int = 10,
        searchText: String = "",
        columnName: String = "requestNumber"
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
     * Get engineer inspection request detail by ID
     * Uses api/v1/scheduled-inspection-requests/{requestId}/details
     * @param requestId Request ID
     */
    suspend fun getEngineerRequestDetail(
        requestId: Int
    ): Result<RequestDetailResponse>

    /**
     * Issue certificate for a request (when isPaid == 1)
     * @param issuanceEndpoint Full issuance endpoint path
     * @return Result with certificate data
     */
    suspend fun issueCertificate(
        issuanceEndpoint: String
    ): Result<RequestDetailResponse>

    /**
     * ✅ Get certificate by certification number (for already issued certificates)
     */
    suspend fun getCertificate(
        certificationNumber: String
    ): Result<RequestDetailResponse>

    /**
     * Refresh requests (clear cache if any)
     */
    suspend fun refreshRequests()
}
