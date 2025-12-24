package com.informatique.mtcit.business.transactions.shared

import com.informatique.mtcit.data.repository.MarineUnitRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Review Manager - Handles review step API calls for all transaction strategies
 *
 * This manager centralizes the logic for calling sendTransactionRequest API
 * but lets each strategy decide how to handle the response.
 *
 * Used by: All transaction strategies (Temporary Registration, Permanent Registration,
 * Mortgage Certificate, Release Mortgage, etc.)
 *
 * ✅ ENHANCED: Supports flexible response handling - each strategy can check different fields:
 * - Temporary Registration: checks needInspection
 * - Mortgage Certificate: might check approvalStatus
 * - Release Mortgage: might check documentVerification
 * - etc.
 */
@Singleton
class ReviewManager @Inject constructor(
    private val marineUnitRepository: MarineUnitRepository
) {

    /**
     * Process review step - Call send-transaction-request API
     *
     * ✅ This method returns the full response data, allowing each strategy to:
     * - Check for needInspection (temporary registration)
     * - Check for approvalStatus (mortgage)
     * - Check for documentVerification (release mortgage)
     * - Or any other field returned by the API
     *
     * @param endpoint The API endpoint (e.g., "temporary-registration")
     * @param requestId The registration request ID
     * @param transactionName The transaction display name for logging
     * @return ReviewResult with success/error and response data
     */
    suspend fun processReviewStep(
        endpoint: String,
        requestId: Int,
        transactionName: String,
        sendRequestPostOrPut: String,
    ): ReviewResult {
        return try {
            println("=" .repeat(80))
            println("📋 ReviewManager: Processing review step")
            println("=" .repeat(80))
            println("   Transaction: $transactionName")
            println("   Endpoint: $endpoint")
            println("   Request ID: $requestId")

            // Call the repository method which internally calls marineUnitsApiService
            val result = marineUnitRepository.sendTransactionRequest(
                endpoint = endpoint,
                requestId = requestId,
                transactionType = transactionName,
                sendRequestPostOrPut = sendRequestPostOrPut
            )

            if (result.isSuccess) {
                val response = result.getOrNull()

                if (response != null) {
                    println("✅ Review API call successful")
                    println("   Message: ${response.message}")
                    println("   Need Inspection: ${response.needInspection}")

                    // ✅ Log additional data if present
                    if (response.additionalData != null) {
                        println("   Additional Data:")
                        response.additionalData.forEach { (key, value) ->
                            println("      $key: $value")
                        }
                    }
                    println("=" .repeat(80))

                    ReviewResult.Success(
                        message = response.message,
                        needInspection = response.needInspection,
                        additionalData = response.additionalData
                    )
                } else {
                    println("❌ Review API returned null response")
                    println("=" .repeat(80))
                    ReviewResult.Error("لم يتم استلام رد من الخادم")
                }
            } else {
                val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                println("❌ Review API call failed: $errorMessage")
                println("=" .repeat(80))
                ReviewResult.Error(errorMessage)
            }
        } catch (e: Exception) {
            println("❌ Exception in ReviewManager.processReviewStep: ${e.message}")
            e.printStackTrace()
            println("=" .repeat(80))
            ReviewResult.Error(e.message ?: "حدث خطأ غير متوقع")
        }
    }
}

/**
 * Result types for review step operations
 */
sealed class ReviewResult {
    /**
     * Success response from send-transaction-request API
     *
     * ✅ ENHANCED: Contains flexible additionalData map for strategy-specific fields
     *
     * @param message Message from server (e.g., "تم إرسال الطلب بنجاح")
     * @param needInspection Whether inspection is required (used by temporary registration)
     * @param additionalData Any additional data from the API (strategy-specific fields)
     *   - For temporary registration: needInspection flag
     *   - For mortgage: approvalStatus, bankVerification, etc.
     *   - For release mortgage: documentVerification, releaseApproval, etc.
     */
    data class Success(
        val message: String,
        val needInspection: Boolean,
        val additionalData: Map<String, Any>? = null
    ) : ReviewResult()

    /**
     * Error response
     *
     * @param message Error message
     */
    data class Error(val message: String) : ReviewResult()
}

/**
 * Response data from send-transaction-request API
 */
data class ReviewResponse(
    val message: String,
    val needInspection: Boolean,
    val additionalData: Map<String, Any>? = null
)
