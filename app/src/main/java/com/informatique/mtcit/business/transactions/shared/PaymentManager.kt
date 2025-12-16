package com.informatique.mtcit.business.transactions.shared

import com.informatique.mtcit.business.transactions.TransactionType
import com.informatique.mtcit.business.transactions.toRequestTypeId
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
     * @return Result indicating success or error
     */
    suspend fun processStepIfNeeded(
        stepType: StepType,
        formData: MutableMap<String, String>,
        requestTypeId: Int
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

            // ✅ Payment Details Step - Load payment receipt
            StepType.PAYMENT -> {
                println("💰 Payment Details step detected - loading payment receipt...")

                // Extract coreShipsInfoId from formData
                val coreShipsInfoId = formData["shipInfoId"] ?: formData["requestId"]

                if (coreShipsInfoId == null) {
                    println("❌ Missing coreShipsInfoId for payment receipt")
                    return StepProcessResult.Error("Missing ship info ID for payment")
                }

                println("📍 Using coreShipsInfoId: $coreShipsInfoId")

                try {
                    val result = getPaymentReceipt(requestTypeId, coreShipsInfoId)

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

            // ✅ Payment Confirmation Step - Submit payment
            StepType.PAYMENT_CONFIRMATION -> {
                println("💳 Payment Confirmation step detected - submitting payment...")

                val receiptJson = formData["paymentReceiptJson"]

                if (receiptJson == null) {
                    println("❌ Missing payment receipt data for submission")
                    return StepProcessResult.Error("Missing payment receipt data")
                }

                try {
                    // Parse the stored receipt
                    val receipt = kotlinx.serialization.json.Json.decodeFromString(
                        PaymentReceipt.serializer(),
                        receiptJson
                    )

                    // Build payment submission request
                    val paymentData = PaymentSubmissionRequest(
                        id = null,
                        receiptSerial = receipt.receiptSerial,
                        receiptYear = receipt.receiptYear,
                        requestId = null, // ✅ NEW: No requestId field in new API
                        requestTypeId = requestTypeId.toString(),
                        penalties = emptyList(),
                        invoiceType = InvoiceTypeIdOnly(id = receipt.invoiceType.id),
                        receiptNo = 0,
                        comments = "",
                        description = "",
                        isPaid = 0,
                        arabicValue = receipt.arabicValue,
                        totalCost = receipt.totalCost,
                        totalTax = receipt.totalTax,
                        finalTotal = receipt.finalTotal,
                        approximateFinalTotal = receipt.approximateFinalTotal,
                        paymentReceiptDetailsList = receipt.paymentReceiptDetailsList.map { detail ->
                            PaymentReceiptDetailSubmission(
                                name = detail.name,
                                value = detail.value,
                                taxValue = detail.taxValue,
                                finalTotal = detail.finalTotal,
                                approximateFinalTotal = detail.approximateFinalTotal,
                                tariffItem = TariffItemIdOnly(
                                    id = detail.tariffItem.id,
                                    name = detail.name
                                ),
                                tariffRate = TariffRateSubmission(
                                    id = detail.tariffRate.id,
                                    tariffItemId = detail.tariffRate.tariffItemId ?: detail.tariffItem.id,
                                    expressionCode = detail.tariffRate.expressionCode,
                                    expressionText = detail.tariffRate.expressionText
                                )
                            )
                        }
                    )

                    val result = submitPayment(requestTypeId.toString(), paymentData)

                    result.fold(
                        onSuccess = { receiptId ->
                            println("✅ Payment submitted successfully! Receipt ID: $receiptId")
                            formData["paymentReceiptId"] = receiptId.toString()
                            formData["paymentSuccessful"] = "true"
                            StepProcessResult.Success("Payment submitted successfully")
                        },
                        onFailure = { error ->
                            println("❌ Failed to submit payment: ${error.message}")
                            formData["paymentSuccessful"] = "false"
                            formData["paymentError"] = error.message ?: "Unknown error"
                            StepProcessResult.Error(error.message ?: "Failed to submit payment")
                        }
                    )
                } catch (e: Exception) {
                    println("❌ Exception submitting payment: ${e.message}")
                    e.printStackTrace()
                    formData["paymentSuccessful"] = "false"
                    formData["paymentError"] = e.message ?: "Unknown error"
                    StepProcessResult.Error("Failed to submit payment: ${e.message}")
                }
            }

            else -> {
                println("ℹ️ Step type $stepType - no payment action needed")
                StepProcessResult.NoAction
            }
        }
    }

    /**
     * Get payment receipt with coreShipsInfoId
     */
    private suspend fun getPaymentReceipt(
        requestTypeId: Int,
        coreShipsInfoId: String
    ): Result<PaymentReceipt> {
        return try {
            println("🚀 PaymentManager: Getting payment receipt...")
            println("   RequestType: $requestTypeId, CoreShipsInfoId: $coreShipsInfoId")

            val result = withContext(Dispatchers.IO) {
                paymentRepository.getPaymentReceipt(requestTypeId, coreShipsInfoId)
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
     * Submit payment
     */
    private suspend fun submitPayment(
        requestTypeId: String,
        paymentData: PaymentSubmissionRequest
    ): Result<Long> {
        return try {
            println("🚀 PaymentManager: Submitting payment for requestType=$requestTypeId...")
            println("   Final Total: ${paymentData.finalTotal}")

            val result = withContext(Dispatchers.IO) {
                paymentRepository.submitPayment(requestTypeId, paymentData)
            }

            result.fold(
                onSuccess = { paymentReceiptId ->
                    println("✅ Payment submitted successfully! Receipt ID: $paymentReceiptId")
                    Result.success(paymentReceiptId)
                },
                onFailure = { exception ->
                    println("❌ Failed to submit payment: ${exception.message}")
                    exception.printStackTrace()
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            println("❌ Exception in submitPayment: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
