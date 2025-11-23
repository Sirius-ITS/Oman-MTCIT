package com.informatique.mtcit.data.repository

import com.informatique.mtcit.business.transactions.TransactionType
import com.informatique.mtcit.business.transactions.shared.MarineUnit
import com.informatique.mtcit.data.model.UserRequest
import com.informatique.mtcit.data.model.RequestStatus
import com.informatique.mtcit.data.model.SaveRequestBody
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing user requests (الاستمارات)
 *
 * Current: Mock implementation with in-memory storage
 * Future: Will call REST API endpoints
 *
 * API Endpoints:
 * - GET /api/users/{userId}/requests → Get all user requests
 * - GET /api/requests/{requestId}/status → Get specific request status
 * - POST /api/requests/save → Save request progress
 * - PUT /api/requests/{requestId}/status → Update request status (admin only)
 */
@Singleton
class RequestRepository @Inject constructor() {

    // ✅ Mock in-memory storage (simulates backend database)
    // In real app, this data comes from API
    private val mockRequests = mutableMapOf<String, UserRequest>()

    init {
        // Pre-populate with sample data for testing
        initializeMockData()
    }

    /**
     * Get all requests for a specific user
     *
     * Mock: Returns from in-memory map
     * API: GET /api/users/{userId}/requests
     */
    suspend fun getUserRequests(userId: String): Result<List<UserRequest>> {
        return try {
            // Simulate network delay
            delay(500)

            // Filter requests by userId
            val userRequests = mockRequests.values
                .filter { it.userId == userId }
                .sortedByDescending { it.lastUpdatedDate }

            println("📋 RequestRepository: Found ${userRequests.size} requests for user $userId")

            Result.success(userRequests)
        } catch (e: Exception) {
            println("❌ RequestRepository: Error getting requests: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Get specific request by ID with latest status
     *
     * Mock: Returns from in-memory map
     * API: GET /api/requests/{requestId}/status
     */
    suspend fun getRequestStatus(requestId: String): Result<UserRequest> {
        return try {
            // Simulate network delay
            delay(300)

            val request = mockRequests[requestId]

            if (request != null) {
                println("✅ RequestRepository: Found request $requestId with status ${request.status}")
                Result.success(request)
            } else {
                println("❌ RequestRepository: Request $requestId not found")
                Result.failure(Exception("الطلب غير موجود"))
            }
        } catch (e: Exception) {
            println("❌ RequestRepository: Error getting request status: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Save request progress (when inspection is pending)
     *
     * Mock: Saves to in-memory map
     * API: POST /api/requests/save
     */
    suspend fun saveRequestProgress(
        userId: String,
        transactionType: TransactionType,
        marineUnit: MarineUnit?,
        formData: Map<String, String>,
        lastCompletedStep: Int,
        status: RequestStatus = RequestStatus.PENDING
    ): Result<String> {
        return try {
            // Simulate network delay
            delay(400)

            // Generate request ID (in real app, comes from backend)
            val requestId = "REQ_${System.currentTimeMillis()}"

            val request = UserRequest(
                id = requestId,
                userId = userId,
                type = transactionType,
                status = status,
                marineUnit = marineUnit,
                createdDate = getCurrentTimestamp(),
                lastUpdatedDate = getCurrentTimestamp(),
                formData = formData,
                lastCompletedStep = lastCompletedStep,
                estimatedCompletionDate = getEstimatedDate(3) // 3 days from now
            )

            // Save to mock storage
            mockRequests[requestId] = request

            println("✅ RequestRepository: Saved request $requestId for user $userId")

            Result.success(requestId)
        } catch (e: Exception) {
            println("❌ RequestRepository: Error saving request: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Update request status (simulates admin approval)
     *
     * Mock: Updates in-memory map
     * API: PUT /api/requests/{requestId}/status (admin only)
     */
    suspend fun updateRequestStatus(
        requestId: String,
        newStatus: RequestStatus,
        rejectionReason: String? = null
    ): Result<Boolean> {
        return try {
            delay(300)

            val request = mockRequests[requestId]

            if (request != null) {
                // Update status
                val updatedRequest = request.copy(
                    status = newStatus,
                    lastUpdatedDate = getCurrentTimestamp(),
                    rejectionReason = rejectionReason
                )

                mockRequests[requestId] = updatedRequest

                println("✅ RequestRepository: Updated request $requestId to status $newStatus")

                Result.success(true)
            } else {
                println("❌ RequestRepository: Request $requestId not found")
                Result.failure(Exception("الطلب غير موجود"))
            }
        } catch (e: Exception) {
            println("❌ RequestRepository: Error updating status: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Delete request (optional - for testing)
     */
    suspend fun deleteRequest(requestId: String): Result<Boolean> {
        return try {
            delay(200)

            val removed = mockRequests.remove(requestId)

            if (removed != null) {
                println("✅ RequestRepository: Deleted request $requestId")
                Result.success(true)
            } else {
                println("❌ RequestRepository: Request $requestId not found")
                Result.failure(Exception("الطلب غير موجود"))
            }
        } catch (e: Exception) {
            println("❌ RequestRepository: Error deleting request: ${e.message}")
            Result.failure(e)
        }
    }

    // ========== Mock Data Helpers ==========

    /**
     * Initialize mock data for testing
     * In real app, this data comes from backend
     */
    private fun initializeMockData() {
        // Sample Request 1: PENDING (waiting for inspection)
        mockRequests["REQ_001"] = UserRequest(
            id = "REQ_001",
            userId = "currentUserId",
            type = TransactionType.TEMPORARY_REGISTRATION_CERTIFICATE,
            status = RequestStatus.PENDING,
            marineUnit = MarineUnit(
                id = "new_123456",
                name = "Sea Falcon",
                type = "سفينة شحن",
                imoNumber = "IMO1234567",
                callSign = "ABC123",
                maritimeId = "",
                registrationPort = "ميناء صحار",
                activity = "Commercial Shipping",
                isOwned = true
            ),
            createdDate = "2025-11-15T10:30:00Z",
            lastUpdatedDate = "2025-11-15T10:30:00Z",
            formData = mapOf(
                "selectionPersonType" to "فرد",
                "callSign" to "ABC123",
                "registrationPort" to "ميناء صحار",
                "unitType" to "سفينة شحن"
            ),
            lastCompletedStep = 8, // Completed up to review step
            estimatedCompletionDate = "2025-11-22T10:30:00Z"
        )

        // Sample Request 2: VERIFIED (inspection approved - can resume)
        // ✅ This simulates: User added NEW unit, completed up to Review step,
        // inspection was done and VERIFIED, now user can continue from Marine Unit Name step
        mockRequests["REQ_002"] = UserRequest(
            id = "REQ_002",
            userId = "currentUserId",
            type = TransactionType.TEMPORARY_REGISTRATION_CERTIFICATE,
            status = RequestStatus.VERIFIED,
            marineUnit = MarineUnit(
                id = "new_789012",
                name = "Ocean Star",
                type = "قارب صيد",
                imoNumber = "",
                callSign = "XYZ789",
                maritimeId = "",
                registrationPort = "ميناء صلالة",
                activity = "Fishing",
                isOwned = true,
                totalLength = "15.5",
                totalWidth = "4.2",
                height = "3.0"
            ),
            createdDate = "2025-11-10T14:20:00Z",
            lastUpdatedDate = "2025-11-18T09:15:00Z",
            formData = mapOf(
                // Step 0: Person Type
                "selectionPersonType" to "فرد",

                // Step 2: Marine Unit Selection (chose to add new)
                "isAddingNewUnit" to "true",
                "selectedMarineUnits" to "[]",

                // Step 3: Unit Selection (new unit details)
                "callSign" to "XYZ789",
                "registrationPort" to "ميناء صلالة",
                "unitType" to "قارب صيد",
                "imoNumber" to "",
                "mmsi" to "123456789",
                "manufacturer" to "Yamaha",
                "maritimeactivity" to "صيد",
                "constructionDate" to "2020-01-01",
                "registrationCountry" to "عمان",

                // Step 4: Dimensions
                "length" to "15.5",
                "width" to "4.2",
                "height" to "3.0",
                "decksCount" to "1",

                // Step 5: Weights
                "grossTonnage" to "25.5",
                "netTonnage" to "20.0",
                "maxPermittedLoad" to "5000",

                // Step 6: Engine Info
                "engineManufacturer" to "Yamaha",
                "engineModel" to "F150",
                "enginePower" to "150",
                "engineManufacturerCountry" to "اليابان",
                "fuelType" to "Diesel",
                "engineCondition" to "New",

                // Step 7: Owner Info
                "ownerName" to "أحمد محمد",
                "ownerNationality" to "عمان",
                "ownerCountry" to "عمان",
                "ownerPhone" to "+96812345678",

                // Step 8: Documents (uploaded)
                "shipbuildingCertificate" to "uploaded",
                "inspectionDocuments" to "uploaded"
            ),
            lastCompletedStep = 8, // Completed Review step, should resume at step 9 (Marine Unit Name)
            inspectionCertificateUrl = "https://example.com/certificates/12345.pdf"
        )

        // Sample Request 3: REJECTED (inspection failed)
        mockRequests["REQ_003"] = UserRequest(
            id = "REQ_003",
            userId = "currentUserId",
            type = TransactionType.TEMPORARY_REGISTRATION_CERTIFICATE,
            status = RequestStatus.REJECTED,
            marineUnit = MarineUnit(
                id = "new_345678",
                name = "Wave Runner",
                type = "يخت",
                imoNumber = "",
                callSign = "WR2024",
                maritimeId = "",
                registrationPort = "ميناء مسقط",
                activity = "Recreation",
                isOwned = true
            ),
            createdDate = "2025-11-05T11:00:00Z",
            lastUpdatedDate = "2025-11-12T16:45:00Z",
            formData = mapOf(
                "selectionPersonType" to "فرد",
                "callSign" to "WR2024",
                "registrationPort" to "ميناء مسقط",
                "unitType" to "يخت"
            ),
            lastCompletedStep = 8,
            rejectionReason = "الوحدة البحرية لا تستوفي متطلبات السلامة البحرية"
        )

        println("✅ RequestRepository: Initialized with ${mockRequests.size} mock requests")
    }

    /**
     * Simulate status change (for testing)
     * In real app, this is done by backend based on admin actions
     */
    fun simulateStatusChange(requestId: String, newStatus: RequestStatus) {
        val request = mockRequests[requestId]
        if (request != null) {
            mockRequests[requestId] = request.copy(
                status = newStatus,
                lastUpdatedDate = getCurrentTimestamp()
            )
            println("🔄 Simulated status change: $requestId → $newStatus")
        }
    }

    // ========== Utility Functions ==========

    private fun getCurrentTimestamp(): String {
        return java.time.Instant.now().toString()
    }

    private fun getEstimatedDate(daysFromNow: Int): String {
        return java.time.Instant.now()
            .plus(java.time.Duration.ofDays(daysFromNow.toLong()))
            .toString()
    }
}
