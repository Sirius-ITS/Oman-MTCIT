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
     * Step 1: Get Invoice Type ID
     * Called BEFORE entering payment step to get the invoice type ID
     *
     * @param requestTypeId Transaction type ID (e.g., "1" for temporary registration)
     * @param requestId The created request ID
     * @return Result with invoice type ID or error
     */
    suspend fun getInvoiceTypeId(
        requestTypeId: String,
        requestId: Long
    ): Result<Long> {
        return try {
            println("🚀 PaymentManager: Getting invoice type ID for requestType=$requestTypeId, requestId=$requestId...")

            val result = withContext(Dispatchers.IO) {
                paymentRepository.getInvoiceTypeId(requestTypeId, requestId)
            }

            result.fold(
                onSuccess = { invoiceTypeId ->
                    println("✅ Invoice Type ID retrieved: $invoiceTypeId")
                    Result.success(invoiceTypeId)
                },
                onFailure = { exception ->
                    println("❌ Failed to get invoice type ID: ${exception.message}")
                    exception.printStackTrace()
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            println("❌ Exception in getInvoiceTypeId: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Step 2: Get Payment Details (Calculate Payment)
     * Called when user enters payment details step
     *
     * @param requestTypeId Transaction type ID to determine correct endpoint
     * @return Result with payment receipt data or error
     */
    suspend fun getPaymentDetails(
        requestTypeId: String
    ): Result<PaymentReceipt> {
        return try {
            println("🚀 PaymentManager: Getting payment details for requestType=$requestTypeId...")

            val result = withContext(Dispatchers.IO) {
                paymentRepository.getPaymentDetails(requestTypeId)
            }

            result.fold(
                onSuccess = { paymentReceipt ->
                    println("✅ Payment details retrieved successfully")
                    println("   Total Cost: ${paymentReceipt.totalCost}")
                    println("   Total Tax: ${paymentReceipt.totalTax}")
                    println("   Final Total: ${paymentReceipt.finalTotal}")
                    Result.success(paymentReceipt)
                },
                onFailure = { exception ->
                    println("❌ Failed to get payment details: ${exception.message}")
                    exception.printStackTrace()
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            println("❌ Exception in getPaymentDetails: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Step 3: Submit Payment
     * Called when user clicks "Pay" button
     *
     * @param requestTypeId Transaction type ID to determine correct endpoint
     * @param paymentData The payment submission data
     * @return Result with payment receipt ID or error
     */
    suspend fun submitPayment(
        requestTypeId: String,
        paymentData: PaymentSubmissionRequest
    ): Result<Long> {
        return try {
            println("🚀 PaymentManager: Submitting payment for requestType=$requestTypeId...")
            println("   Request ID: ${paymentData.requestId}")
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

    /**
     * Helper: Get Core Ship Data from API
     * Used to fetch ship details for payment submission
     *
     * @param requestTypeId Transaction type ID
     * @param requestId The created request ID
     * @return Result with core ship data or error
     */
    suspend fun getCoreShipData(
        requestTypeId: String,
        requestId: Long
    ): Result<CoreShipsDto> {
        return try {
            println("🚀 PaymentManager: Getting core ship data for requestType=$requestTypeId, requestId=$requestId...")

            val result = withContext(Dispatchers.IO) {
                paymentRepository.getCoreShipData(requestTypeId, requestId)
            }

            result.fold(
                onSuccess = { coreShipData ->
                    println("✅ Core ship data retrieved: ${coreShipData.shipName} (ID: ${coreShipData.id})")
                    Result.success(coreShipData)
                },
                onFailure = { exception ->
                    println("❌ Failed to get core ship data: ${exception.message}")
                    exception.printStackTrace()
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            println("❌ Exception in getCoreShipData: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Helper: Build payment submission request from payment receipt and core ship data
     */
    fun buildPaymentSubmissionRequest(
        paymentReceipt: PaymentReceipt,
        requestId: Long,
        requestTypeId: String
    ): PaymentSubmissionRequest {
        return PaymentSubmissionRequest(
            id = null,
            receiptSerial = paymentReceipt.receiptSerial,
            receiptYear = paymentReceipt.receiptYear,
            requestId = requestId,
            requestTypeId = requestTypeId,
            penalties = emptyList(),
            invoiceType = InvoiceTypeIdOnly(id = paymentReceipt.invoiceType.id),
            receiptNo = 0,
            comments = "",
            description = "",
            isPaid = 0,
            arabicValue = paymentReceipt.arabicValue,
            totalCost = paymentReceipt.totalCost,
            totalTax = paymentReceipt.totalTax,
            finalTotal = paymentReceipt.finalTotal,
            approximateFinalTotal = paymentReceipt.approximateFinalTotal,
            paymentReceiptDetailsList = paymentReceipt.paymentReceiptDetailsList.map { detail ->
                PaymentReceiptDetailSubmission(
                    name = detail.name,
                    value = detail.value,
                    taxValue = detail.taxValue,
                    finalTotal = detail.finalTotal,
                    approximateFinalTotal = detail.approximateFinalTotal,
                    tariffItem = TariffItemIdOnly(
                        id = detail.tariffItem.id,
                        name = detail.tariffItem.nameAr ?: detail.name
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
    }
}
