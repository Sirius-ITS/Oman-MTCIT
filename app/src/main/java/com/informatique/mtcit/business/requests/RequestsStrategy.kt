package com.informatique.mtcit.business.requests

import androidx.compose.ui.graphics.Color
import com.informatique.mtcit.business.transactions.TransactionType
import com.informatique.mtcit.data.model.RequestStatus
import com.informatique.mtcit.data.model.requests.PaginationState
import com.informatique.mtcit.data.model.requests.RequestItem
import com.informatique.mtcit.data.model.requests.RequestsApiResponse
import com.informatique.mtcit.data.model.requests.UserRequestUiModel
import com.informatique.mtcit.data.repository.UserRequestsRepository
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Business Logic Strategy for User Requests
 * Handles data transformation, status mapping, and localization
 */
@Singleton
class RequestsStrategy @Inject constructor(
    private val repository: UserRequestsRepository
) {

    /**
     * Load user requests with pagination
     */
    suspend fun loadUserRequests(
        civilId: String,
        size: Int = 10,
        page: Int = 0,
        sort : String = "lastChange,desc"
    ): Result<RequestsResult> {
        return try {
            println("🎯 RequestsStrategy: Loading requests for civilId=$civilId, page=$page, sort=$sort")

            val result = repository.getUserRequests(civilId, size, page, sort)  // ✅ Pass sort parameter

            result.fold(
                onSuccess = { apiResponse ->
                    val uiModels = apiResponse.data?.content?.map { item ->
                        mapToUiModel(item)
                    } ?: emptyList()

                    val paginationState = PaginationState(
                        currentPage = apiResponse.data?.number ?: 0,
                        totalPages = apiResponse.data?.totalPages ?: 0,
                        totalElements = apiResponse.data?.totalElements ?: 0,
                        isLastPage = apiResponse.data?.last ?: true,
                        isFirstPage = apiResponse.data?.first ?: true,
                        hasMore = !(apiResponse.data?.last ?: true)
                    )

                    println("✅ RequestsStrategy: Mapped ${uiModels.size} requests to UI models")

                    Result.success(RequestsResult(uiModels, paginationState))
                },
                onFailure = { error ->
                    println("❌ RequestsStrategy: Failed to load requests: ${error.message}")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            println("❌ RequestsStrategy: Exception: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Load filtered user requests with pagination
     */
    suspend fun loadFilteredUserRequests(
        civilId: String,
        filter: com.informatique.mtcit.data.model.requests.RequestFilterDto
    ): Result<RequestsResult> {
        return try {
            println("🎯 RequestsStrategy: Loading filtered requests - statusId=${filter.statusId}, page=${filter.page}")

            val result = repository.getFilteredUserRequests(civilId, filter)

            result.fold(
                onSuccess = { apiResponse ->
                    val uiModels = apiResponse.data?.content?.map { item ->
                        mapToUiModel(item)
                    } ?: emptyList()

                    val paginationState = PaginationState(
                        currentPage = apiResponse.data?.number ?: 0,
                        totalPages = apiResponse.data?.totalPages ?: 0,
                        totalElements = apiResponse.data?.totalElements ?: 0,
                        isLastPage = apiResponse.data?.last ?: true,
                        isFirstPage = apiResponse.data?.first ?: true,
                        hasMore = !(apiResponse.data?.last ?: true)
                    )

                    println("✅ RequestsStrategy: Mapped ${uiModels.size} filtered requests to UI models")

                    Result.success(RequestsResult(uiModels, paginationState))
                },
                onFailure = { error ->
                    println("❌ RequestsStrategy: Failed to load filtered requests: ${error.message}")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            println("❌ RequestsStrategy: Exception: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Load engineer inspection requests with pagination
     */
    suspend fun loadEngineerInspectionRequests(
        page: Int = 0,
        size: Int = 10,
        searchText: String = "",
        columnName: String = "requestNumber"
    ): Result<RequestsResult> {
        return try {
            println("🎯 RequestsStrategy: Loading engineer inspection requests - page=$page")

            val result = repository.getEngineerInspectionRequests(page, size, searchText, columnName)

            result.fold(
                onSuccess = { apiResponse ->
                    val uiModels = apiResponse.data?.content?.map { item ->
                        mapToUiModel(item)
                    } ?: emptyList()

                    val paginationState = PaginationState(
                        currentPage = apiResponse.data?.number ?: 0,
                        totalPages = apiResponse.data?.totalPages ?: 0,
                        totalElements = apiResponse.data?.totalElements ?: 0,
                        isLastPage = apiResponse.data?.last ?: true,
                        isFirstPage = apiResponse.data?.first ?: true,
                        hasMore = !(apiResponse.data?.last ?: true)
                    )

                    println("✅ RequestsStrategy: Mapped ${uiModels.size} engineer inspection requests to UI models")

                    Result.success(RequestsResult(uiModels, paginationState))
                },
                onFailure = { error ->
                    println("❌ RequestsStrategy: Failed to load engineer inspection requests: ${error.message}")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            println("❌ RequestsStrategy: Exception in filtered requests: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Map API RequestItem to UI Model with localized status
     */
    private fun mapToUiModel(item: RequestItem): UserRequestUiModel {
        // ✅ Use helper methods to get actual values from either flat fields or nested objects
        val statusId = item.getActualStatusId()
        val statusName = item.getActualStatusName()
        val shipName = item.getActualShipName()
        val requestNumber = item.getDisplayRequestNumber()

        val statusEnum = RequestStatus.fromStatusId(statusId)
        val localizedStatusName = statusEnum?.getLocalizedName()
            ?: statusName
            ?: RequestStatus.getStatusName(statusId)

        val (statusColor, statusBgColor) = getStatusColors(statusId)

        // ✅ Use improved method that handles all cases
        val requestTypeName = item.getRequestTypeDisplayName()
        val requestTypeId = item.requestTypeId ?: item.requestType?.id ?: 8 // Default to inspection (8)

        println("🔍 Mapping request: id=${item.id}, requestNumber=$requestNumber, statusId=$statusId, statusName=$localizedStatusName, shipName=$shipName, requestType=$requestTypeName")

        return UserRequestUiModel(
            id = (item.requestId ?: item.id)!!,
            requestSerial = requestNumber,
            requestTypeId = requestTypeId,
            requestTypeName = requestTypeName,
            shipId = item.shipId ?: item.ship?.id ?: item.shipInfo?.ship?.id,
            shipName = shipName ?: getLocalizedText("no_ship"),
            shipNumber = item.shipNumber ?: "",
            statusId = statusId,
            statusName = localizedStatusName,
            statusColor = statusColor,
            statusBgColor = statusBgColor,
            creationDate = item.createdAt ?: item.creationDate ?: "",
            modificationDate = item.lastChange ?: item.modificationDate ?: item.createdAt ?: item.creationDate ?: "",
            rejectionReason = item.rejectionReason
        )
    }

    /**
     * Get status colors based on statusId
     */
    private fun getStatusColors(statusId: Int): Pair<Color, Color> {
        return when (statusId) {
            1 -> Pair(Color(0xFF9E9E9E), Color(0xFFF5F5F5)) // DRAFT - Gray
            2, 10 -> Pair(Color(0xFFF44336), Color(0xFFFFE8E8)) // REJECTED - Red
            3, 7, 11, 12 -> Pair(Color(0xFF4CAF50), Color(0xFFE8F5E9)) // CONFIRMED/ACCEPTED/APPROVED - Green
            4, 5 -> Pair(Color(0xFFFF9800), Color(0xFFFFF3E0)) // SEND/PENDING - Orange
            6 -> Pair(Color(0xFF2196F3), Color(0xFFE3F2FD)) // SCHEDULED - Blue
            8, 9, 15 -> Pair(Color(0xFF4A90E2), Color(0xFFE8F4FD)) // IN_REVIEW - Blue
            13, 14 -> Pair(Color(0xFF4CAF50), Color(0xFFE8F5E9)) // ACTION_TAKEN/ISSUED - Green
            16 -> Pair(Color(0xFFFF9800), Color(0xFFFFF3E0)) // WAITING_INSPECTION - Orange
            else -> Pair(Color(0xFF757575), Color(0xFFF0F0F0)) // Unknown - Gray
        }
    }

    /**
     * Get localized text
     */
    private fun getLocalizedText(key: String): String {
        val isArabic = Locale.getDefault().language == "ar"
        return when (key) {
            "unknown_request_type" -> if (isArabic) "نوع طلب غير معروف" else "Unknown Request Type"
            "no_ship" -> if (isArabic) "لا توجد سفينة" else "No Ship"
            else -> if (isArabic) "غير معروف" else "Unknown"
        }
    }

    /**
     * Refresh requests
     */
    suspend fun refreshRequests() {
        repository.refreshRequests()
    }
}

/**
 * Result wrapper containing UI models and pagination info
 */
data class RequestsResult(
    val requests: List<UserRequestUiModel>,
    val pagination: PaginationState
)
