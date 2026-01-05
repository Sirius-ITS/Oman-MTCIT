package com.informatique.mtcit.data.repository

import com.informatique.mtcit.data.api.RequestsApiService
import com.informatique.mtcit.data.model.requests.RequestsApiResponse
import com.informatique.mtcit.data.model.requests.RequestDetailResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRequestsRepositoryImpl @Inject constructor(
    private val requestsApiService: RequestsApiService
) : UserRequestsRepository {

    override suspend fun getUserRequests(
        civilId: String,
        size: Int,
        page: Int
    ): Result<RequestsApiResponse> = withContext(Dispatchers.IO) {
        try {
            println("📦 UserRequestsRepository: Fetching requests for civilId=$civilId, page=$page, size=$size")

            requestsApiService.getUserRequests(
                civilId = civilId,
                size = size,
                page = page
            )
        } catch (e: Exception) {
            println("❌ UserRequestsRepository: Error fetching requests: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun getRequestDetail(
        requestId: Int,
        endpointPath: String
    ): Result<RequestDetailResponse> = withContext(Dispatchers.IO) {
        try {
            println("📦 UserRequestsRepository: Fetching detail for requestId=$requestId, endpoint=$endpointPath")

            requestsApiService.getRequestDetail(
                requestId = requestId,
                endpointPath = endpointPath
            )
        } catch (e: Exception) {
            println("❌ UserRequestsRepository: Error fetching request detail: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun issueCertificate(
        issuanceEndpoint: String
    ): Result<RequestDetailResponse> = withContext(Dispatchers.IO) {
        try {
            println("📦 UserRequestsRepository: Issuing certificate for endpoint=$issuanceEndpoint")

            requestsApiService.issueCertificate(
                issuanceEndpoint = issuanceEndpoint
            )
        } catch (e: Exception) {
            println("❌ UserRequestsRepository: Error issuing certificate: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun refreshRequests() {
        // Can implement cache clearing here if needed in the future
        println("🔄 UserRequestsRepository: Refresh requests (cache clear if implemented)")
    }
}
