package com.informatique.mtcit.business.transactions.shared

import com.informatique.mtcit.data.model.*
import com.informatique.mtcit.data.repository.PaymentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared manager for handling payment-related API calls
 * Follows the same pattern as RegistrationRequestManager
 */
@Singleton
class PaymentManager @Inject constructor(
    private val paymentRepository: PaymentRepository
) {

    /**
     * Process step based on StepType and call appropriate API
     * Similar to RegistrationRequestManager.processStepIfNeeded()
     *
     * @param stepType The type of the current step
     * @param formData All accumulated form data
     * @param requestTypeId Transaction type ID (e.g., 1 for temporary registration)
     * @param context Transaction context containing API endpoints
     * @return Result indicating success or error
     */
    suspend fun processStepIfNeeded(
        stepType: StepType,
        formData: MutableMap<String, String>,
        requestTypeId: Int,
        context: com.informatique.mtcit.business.transactions.TransactionContext
    ): StepProcessResult {
        println("🔍 PaymentManager.processStepIfNeeded called")
        println("   - stepType: $stepType")
        println("   - requestTypeId: $requestTypeId")
        println("   - formData keys: ${formData.keys}")

        return when (stepType) {
            // ✅ Marine Unit Name Selection Step - Name reservation is handled by RegistrationRequestManager
            StepType.MARINE_UNIT_NAME_SELECTION -> {
                println("ℹ️ Marine Unit Name Selection step - handled by RegistrationRequestManager")
                StepProcessResult.NoAction
            }

            // ✅ Payment Details Step - Load payment receipt OR submit payment
            StepType.PAYMENT -> {
                println("💰 Payment step detected...")

                // ✅ Get payment endpoint from context
                val paymentEndpoint = context.paymentReceiptEndpoint
                if (paymentEndpoint == null) {
                    println("❌ No payment receipt endpoint configured for this transaction")
                    return StepProcessResult.Error("Payment endpoint not configured")
                }

                // Extract requestId and coreShipsInfoId from formData
                val requestId = formData["requestId"]
                val coreShipsInfoId = formData["shipInfoId"] ?: formData["coreShipsInfoId"]

                if (requestId == null || coreShipsInfoId == null) {
                    println("❌ Missing requestId or coreShipsInfoId for payment")
                    println("   requestId: $requestId")
                    println("   coreShipsInfoId: $coreShipsInfoId")
                    return StepProcessResult.Error("Missing required payment data")
                }

                println("📍 Using requestId: $requestId, coreShipsInfoId: $coreShipsInfoId")

                // ✅ Smart detection: If payment details are already loaded, this is a SUBMIT action
                // Otherwise, this is the initial LOAD action
                val isPaymentAlreadyLoaded = formData["paymentFinalTotal"] != null

                return if (isPaymentAlreadyLoaded) {
                    println("💳 Payment details already loaded - User clicked Pay button - submitting payment...")

                    // Get payment submit endpoint
                    val paymentSubmitEndpoint = context.paymentSubmitEndpoint
                    if (paymentSubmitEndpoint == null) {
                        println("❌ No payment submit endpoint configured")
                        StepProcessResult.Error("Payment submit endpoint not configured")
                    } else {
                        try {
                            val result = submitPayment(
                                endpoint = paymentSubmitEndpoint,
                                requestTypeId = requestTypeId,
                                requestId = requestId.toInt(),
                                coreShipsInfoId = coreShipsInfoId
                            )

                            result.fold(
                                onSuccess = { paymentResponse ->
                                    println("✅ Payment submitted successfully!")
                                    println("   Receipt ID: ${paymentResponse.data}")
                                    println("   Message: ${paymentResponse.message}")
                                    println("   Timestamp: ${paymentResponse.timestamp}")

                                    // Store payment success data in formData
                                    formData["paymentReceiptId"] = paymentResponse.data.toString()
                                    formData["paymentSuccessMessage"] = paymentResponse.message
                                    formData["paymentTimestamp"] = paymentResponse.timestamp
                                    formData["showPaymentSuccessDialog"] = "true"

                                    StepProcessResult.Success("Payment submitted successfully")
                                },
                                onFailure = { error ->
                                    println("❌ Failed to submit payment: ${error.message}")
                                    StepProcessResult.Error(error.message ?: "Failed to submit payment")
                                }
                            )
                        } catch (e: Exception) {
                            println("❌ Exception submitting payment: ${e.message}")
                            e.printStackTrace()
                            StepProcessResult.Error("Failed to submit payment: ${e.message}")
                        }
                    }
                } else {
                    println("📄 Loading payment details for the first time...")

                    // Load payment receipt (initial step entry)
                    try {
                        val result = getPaymentReceipt(
                            endpoint = paymentEndpoint,
                            requestTypeId = requestTypeId,
                            requestId = requestId,
                            coreShipsInfoId = coreShipsInfoId
                        )

                        result.fold(
                            onSuccess = { receipt ->
                                println("✅ Payment receipt loaded successfully")

                                // Store receipt data in formData
                                formData["paymentReceiptSerial"] = receipt.receiptSerial.toString()
                                formData["paymentReceiptYear"] = receipt.receiptYear.toString()
                                formData["paymentTotalCost"] = receipt.totalCost.toString()
                                formData["paymentTotalTax"] = receipt.totalTax.toString()
                                formData["paymentFinalTotal"] = receipt.finalTotal.toString()
                                formData["paymentArabicValue"] = receipt.arabicValue
                                formData["paymentInvoiceTypeId"] = receipt.invoiceType.id.toString()

                                // Store full receipt as JSON for later submission
                                val receiptJson = kotlinx.serialization.json.Json.encodeToString(
                                    PaymentReceipt.serializer(),
                                    receipt
                                )
                                formData["paymentReceiptJson"] = receiptJson

                                StepProcessResult.Success("Payment details loaded successfully")
                            },
                            onFailure = { error ->
                                println("❌ Failed to load payment receipt: ${error.message}")
                                StepProcessResult.Error(error.message ?: "Failed to load payment details")
                            }
                        )
                    } catch (e: Exception) {
                        println("❌ Exception loading payment receipt: ${e.message}")
                        e.printStackTrace()
                        StepProcessResult.Error("Failed to load payment details: ${e.message}")
                    }
                }
            }

            else -> {
                println("ℹ️ Step type $stepType - no payment action needed")
                StepProcessResult.NoAction
            }
        }
    }

    /**
     * Get payment receipt with endpoint, requestType, requestId, and coreShipsInfoId
     */
    private suspend fun getPaymentReceipt(
        endpoint: String,
        requestTypeId: Int,
        requestId: String,
        coreShipsInfoId: String
    ): Result<PaymentReceipt> {
        return try {
            println("🚀 PaymentManager: Getting payment receipt...")
            println("   Endpoint: $endpoint")
            println("   RequestType: $requestTypeId, RequestId: $requestId, CoreShipsInfoId: $coreShipsInfoId")

            val result = withContext(Dispatchers.IO) {
                paymentRepository.getPaymentReceipt(endpoint, requestTypeId, requestId, coreShipsInfoId)
            }

            result.fold(
                onSuccess = { paymentReceipt ->
                    println("✅ Payment receipt retrieved successfully")
                    println("   Total Cost: ${paymentReceipt.totalCost}")
                    println("   Total Tax: ${paymentReceipt.totalTax}")
                    println("   Final Total: ${paymentReceipt.finalTotal}")
                    Result.success(paymentReceipt)
                },
                onFailure = { exception ->
                    println("❌ Failed to get payment receipt: ${exception.message}")
                    exception.printStackTrace()
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            println("❌ Exception in getPaymentReceipt: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Submit payment using the simple payment submission API
     */
    private suspend fun submitPayment(
        endpoint: String,
        requestTypeId: Int,
        requestId: Int,
        coreShipsInfoId: String
    ): Result<PaymentResponse<Long>> {
        return try {
            println("🚀 PaymentManager: Submitting payment (simple API)...")
            println("   Endpoint: $endpoint")
            println("   RequestTypeId: $requestTypeId")
            println("   RequestId: $requestId")
            println("   CoreShipsInfoId: $coreShipsInfoId")

            val result = withContext(Dispatchers.IO) {
                paymentRepository.submitPayment(endpoint, requestTypeId, requestId, coreShipsInfoId)
            }

            result.fold(
                onSuccess = { paymentResponse ->
                    println("✅ Payment submitted successfully (simple API)!")
                    Result.success(paymentResponse)
                },
                onFailure = { exception ->
                    println("❌ Failed to submit payment (simple API): ${exception.message}")
                    exception.printStackTrace()
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            println("❌ Exception in submitSimplePayment: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
