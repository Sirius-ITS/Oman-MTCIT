package com.informatique.mtcit.business.transactions.shared

import com.informatique.mtcit.business.transactions.TransactionContext
import com.informatique.mtcit.data.api.MarineUnitsApiService
import com.informatique.mtcit.data.model.ProceedRequestResponse
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ✅ Ship Selection Manager
 * Handles proceed-request API call when user selects a ship and clicks Next
 *
 * This manager centralizes the logic for:
 * - Calling proceed-request API
 * - Handling success/failure
 * - Storing request ID
 * - Error handling
 *
 * Usage in Strategy:
 * ```
 * if (currentStepData?.titleRes == R.string.owned_ships) {
 *     val result = shipSelectionManager.handleShipSelection(
 *         shipId = shipId,
 *         context = transactionContext
 *     )
 *
 *     when (result) {
 *         is ShipSelectionResult.Success -> {
 *             createdRequestId = result.requestId
 *             // Continue to next step
 *         }
 *         is ShipSelectionResult.Error -> {
 *             lastApiError = result.message
 *             return -1  // Block navigation
 *         }
 *     }
 * }
 * ```
 */
@Singleton
class ShipSelectionManager @Inject constructor(
    private val marineUnitsApiService: MarineUnitsApiService
) {

    /**
     * Handle ship selection and proceed-request API call
     *
     * @param shipId The selected ship info ID (will be cleaned automatically)
     * @param context The transaction context containing the proceed endpoint
     * @return ShipSelectionResult indicating success or failure
     */
    suspend fun handleShipSelection(
        shipId: String?,
        context: TransactionContext
    ): ShipSelectionResult {
        println("=".repeat(80))
        println("🚢 ShipSelectionManager: Handling ship selection")
        println("=".repeat(80))

        // ✅ Validate ship ID
        if (shipId.isNullOrBlank() || shipId == "[]") {
            println("❌ No ship selected")
            return ShipSelectionResult.Error(
                message = "يرجى اختيار سفينة",
                shouldBlockNavigation = true
            )
        }

        // ✅ Clean ship ID (remove brackets, quotes, whitespace)
        val cleanShipId = shipId.trim().removeSurrounding("[", "]").trim()
        println("   Selected Ship ID: $cleanShipId")

        // ✅ Get proceed endpoint from context
        val proceedEndpoint = context.proceedRequestEndpoint
        if (proceedEndpoint.isNullOrBlank()) {
            println("❌ No proceed endpoint configured for this transaction")
            return ShipSelectionResult.Error(
                message = "خطأ في إعدادات المعاملة - لا يوجد endpoint",
                shouldBlockNavigation = true
            )
        }

        println("   Endpoint: $proceedEndpoint")
        println("   Transaction: ${context.displayName}")

        // ✅ Call proceed-request API
        return try {
            val result = marineUnitsApiService.proceedWithRequest(
                endpoint = proceedEndpoint,
                shipInfoId = cleanShipId,
                transactionType = context.displayName
            )

            result.fold(
                onSuccess = { response ->
                    println("✅ Proceed-request API successful!")
                    println("   Request ID: ${response.data.id}")
                    println("   Request Serial: ${response.data.requestSerial}")
                    println("   Message: ${response.message}")
                    println("=".repeat(80))

                    ShipSelectionResult.Success(
                        requestId = response.data.id,
                        message = response.message,
                        response = response
                    )
                },
                onFailure = { error ->
                    println("❌ Proceed-request API failed: ${error.message}")
                    error.printStackTrace()
                    println("=".repeat(80))

                    ShipSelectionResult.Error(
                        message = error.message ?: "فشل في متابعة الطلب",
                        shouldBlockNavigation = true
                    )
                }
            )
        } catch (e: Exception) {
            println("❌ Exception in proceed-request API: ${e.message}")
            e.printStackTrace()
            println("=".repeat(80))

            ShipSelectionResult.Error(
                message = e.message ?: "حدث خطأ غير متوقع",
                shouldBlockNavigation = true
            )
        }
    }
}

/**
 * Result of ship selection and proceed-request API call
 */
sealed class ShipSelectionResult {
    /**
     * Success - proceed-request API returned 200 OK
     *
     * @param requestId The created request ID from the API
     * @param message Success message from the API
     * @param response The full API response (optional, for additional data)
     */
    data class Success(
        val requestId: Int,
        val message: String,
        val response: ProceedRequestResponse
    ) : ShipSelectionResult()

    /**
     * Error - proceed-request API failed or validation error
     *
     * @param message Error message to display to user
     * @param shouldBlockNavigation Whether to prevent navigation to next step (usually true)
     */
    data class Error(
        val message: String,
        val shouldBlockNavigation: Boolean = true
    ) : ShipSelectionResult()
}

