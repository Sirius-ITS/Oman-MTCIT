package com.informatique.mtcit.business.transactions.shared

import com.informatique.mtcit.data.model.*
import com.informatique.mtcit.data.model.requests.RequestTypeEndpoint
import com.informatique.mtcit.data.repository.PaymentRepository
import com.informatique.mtcit.data.repository.UserRequestsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared manager for handling payment-related API calls
 * Follows the same pattern as RegistrationRequestManager
 */
@Singleton
class PaymentManager @Inject constructor(
    private val paymentRepository: PaymentRepository,
    private val userRequestsRepository: UserRequestsRepository
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

                // ✅ Check if user is clicking "Issue Certificate" button (for free or already-paid services)
                val shouldIssueCertificate = formData["shouldIssueCertificate"]?.toBoolean() ?: false
                val paymentCompleted = formData["paymentCompleted"]?.toBoolean() ?: false // ✅ NEW: Check if payment just completed
                val certificateNotYetIssued = formData["certificateIssued"] != "true"

                if ((shouldIssueCertificate || paymentCompleted) && certificateNotYetIssued) {
                    println("🎫 User clicked 'Issue Certificate' button...")
                    println("   shouldIssueCertificate: $shouldIssueCertificate")
                    println("   paymentCompleted: $paymentCompleted")
                    println("   Step 1: Calling add-payment API first (even for free services)")

                    // Get payment submit endpoint
                    val paymentSubmitEndpoint = context.paymentSubmitEndpoint
                    if (paymentSubmitEndpoint == null) {
                        println("❌ No payment submit endpoint configured")
                        return StepProcessResult.Error("Payment submit endpoint not configured")
                    }

                    val requestIdInt = requestId.toIntOrNull()
                    if (requestIdInt == null) {
                        println("❌ Invalid requestId for payment/certificate")
                        return StepProcessResult.Error("Invalid request ID")
                    }

                    try {
                        // ✅ Step 1: Call add-payment API (to record payment in system)
                        val paymentResult = submitPayment(
                            endpoint = paymentSubmitEndpoint,
                            requestTypeId = requestTypeId,
                            requestId = requestIdInt,
                            coreShipsInfoId = coreShipsInfoId
                        )

                        return paymentResult.fold(
                            onSuccess = { paymentResponse ->
                                println("✅ Step 1 Complete: Payment recorded in system")
                                println("   Receipt ID: ${paymentResponse.data}")

                                // Store payment info
                                formData["paymentReceiptId"] = paymentResponse.data.toString()
                                formData["paymentTimestamp"] = paymentResponse.timestamp

                                // ✅ Step 2: Issue certificate (skip payment gateway for free/paid services)
                                println("   Step 2: Issuing certificate...")
                                val certResult = issueCertificate(requestTypeId, requestIdInt)

                                certResult.fold(
                                    onSuccess = { certificateUrl ->
                                        println("✅ Step 2 Complete: Certificate issued successfully!")
                                        formData["certificateUrl"] = certificateUrl
                                        formData["certificateIssued"] = "true"
                                        formData["shouldShowCertificate"] = "true"
                                        StepProcessResult.Success("Certificate issued successfully")
                                    },
                                    onFailure = { certError ->
                                        println("❌ Step 2 Failed: Certificate issuance error: ${certError.message}")
                                        StepProcessResult.Error(certError.message ?: "Failed to issue certificate")
                                    }
                                )
                            },
                            onFailure = { paymentError ->
                                println("❌ Step 1 Failed: Payment recording error: ${paymentError.message}")
                                StepProcessResult.Error(paymentError.message ?: "Failed to record payment")
                            }
                        )
                    } catch (e: Exception) {
                        println("❌ Exception during payment/certificate process: ${e.message}")
                        e.printStackTrace()
                        return StepProcessResult.Error("Failed to process: ${e.message}")
                    }
                }

                // ✅ Smart detection: If payment details are already loaded, this is a SUBMIT action
                // Otherwise, this is the initial LOAD action
                val isPaymentAlreadyLoaded = formData["paymentFinalTotal"] != null

                // ✅ NEW: Check if this is a payment retry (user clicked Continue in retry dialog)
                val isPaymentRetry = formData["_triggerPaymentRetry"]?.toBoolean() ?: false
                if (isPaymentRetry) {
                    println("🔄 Payment retry triggered by user")
                    formData.remove("_triggerPaymentRetry") // Clear the trigger
                }

                // ✅ NEW: If user is retrying payment and paymentStatus=1 exists, skip submitPayment
                // and go directly to payment gateway flow
                val existingPaymentStatus = formData["paymentStatus"]?.toIntOrNull()
                val existingReceiptId = formData["paymentReceiptId"]?.toString()?.toLongOrNull()

                println("🔍 Retry check:")
                println("   isPaymentRetry: $isPaymentRetry")
                println("   existingPaymentStatus: $existingPaymentStatus")
                println("   existingReceiptId: $existingReceiptId")
                println("   formData paymentReceiptId: ${formData["paymentReceiptId"]}")

                if (isPaymentRetry && existingPaymentStatus == 1 && existingReceiptId != null) {
                    println("🔄 User chose to retry payment - payment already in progress")
                    println("   Skipping submitPayment, going directly to payment gateway")
                    println("   Using existing receiptId: $existingReceiptId")

                    // Clear the retry dialog flag
                    formData["showPaymentRetryDialog"] = "false"

                    // Prepare payment redirect directly
                    try {
                        val successUrl = "mtcit://payment/success"
                        val canceledUrl = "mtcit://payment/cancel"

                        val htmlResult = withContext(Dispatchers.IO) {
                            paymentRepository.preparePaymentRedirect(
                                existingReceiptId,
                                successUrl,
                                canceledUrl,
                                existingPaymentStatus
                            )
                        }

                        return@processStepIfNeeded htmlResult.fold(
                            onSuccess = { html ->
                                // Trigger in-app WebView
                                formData["paymentRedirectHtml"] = html
                                formData["paymentRedirectSuccessUrl"] = successUrl
                                formData["paymentRedirectCanceledUrl"] = canceledUrl
                                formData["_triggerPaymentWebView"] = "true"
                                println("✅ Payment gateway redirect prepared for retry")
                                StepProcessResult.Success("Payment gateway ready")
                            },
                            onFailure = { error ->
                                println("❌ Failed to prepare payment redirect for retry: ${error.message}")
                                StepProcessResult.Error(error.message ?: "Failed to prepare payment redirect")
                            }
                        )
                    } catch (e: Exception) {
                        println("❌ Exception preparing payment redirect for retry: ${e.message}")
                        return@processStepIfNeeded StepProcessResult.Error(e.message ?: "Failed to prepare payment")
                    }
                }

                return if (isPaymentAlreadyLoaded || isPaymentRetry) {
                    if (isPaymentRetry) {
                        println("🔄 User chose to retry payment - re-submitting with paymentStatus...")
                    } else {
                        println("💳 Payment details already loaded - User clicked Pay button - submitting payment...")
                    }

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
                                    println("   API isPaid: ${paymentResponse.isPaid ?: "not specified"}")

                                    // Store receipt id and timestamp
                                    formData["paymentReceiptId"] = paymentResponse.data.toString()
                                    formData["paymentTimestamp"] = paymentResponse.timestamp
                                    formData["paymentSuccessMessage"] = paymentResponse.message

                                    // ✅ NEW: Extract paymentStatus from response if present
                                    val paymentStatus = paymentResponse.paymentStatus
                                    println("🔍 Payment Status from API: ${paymentStatus ?: "not specified"}")

                                    // ✅ NEW: Check if payment is in progress (paymentStatus = 1)
                                    // This means the request was submitted for payment before but bank response hasn't been received yet
                                    if (paymentStatus == 1) {
                                        println("⏳ Payment in progress detected (paymentStatus=1)")
                                        println("   User needs to confirm if they want to retry payment")
                                        formData["showPaymentRetryDialog"] = "true"
                                        formData["paymentStatus"] = paymentStatus.toString()
                                        // Dialog will be shown in UI, user can choose to continue or close
                                        // If user continues, payment flow will retry from beginning
                                        return@fold StepProcessResult.Success("Payment in progress - showing retry dialog")
                                    }

                                    // ✅ NEW: Check if payment response indicates already paid (isPaid = "1")
                                    // This happens when admin/system has already marked the payment as complete
                                    // Or when the API returns isPaid="1" in the response (e.g., navigation license)
                                    val isPaidFromApi = paymentResponse.isPaid
                                    println("💰 Checking payment status from API: isPaid = $isPaidFromApi")

                                    if (isPaidFromApi == "1") {
                                        println("✅ Payment already completed (API returned isPaid=1)")
                                        println("   Setting flags for manual certificate issuance via button")
                                        formData["isPaid"] = "1"
                                        formData["paymentAlreadyCompleted"] = "true"
                                        formData["shouldIssueCertificate"] = "true"
                                        // Don't auto-issue - let user click button to issue
                                        // Skip payment gateway redirect
                                        return@fold StepProcessResult.Success("Payment completed - ready for certificate issuance")
                                    }

                                    // Payment needs to go through payment gateway
                                    println("💰 Payment requires gateway processing (isPaid=${isPaidFromApi ?: "0"})")

                                    // Prepare payment redirect (HTML form to send user to payment gateway)
                                    try {
                                        val receiptId = paymentResponse.data
                                        // Use a deep link that the gateway will redirect to; WebView will intercept it
                                        val successUrl = "mtcit://payment/success"
                                        val canceledUrl = "mtcit://payment/cancel"

                                        // ✅ Pass paymentStatus to preparePaymentRedirect
                                        val htmlResult = withContext(Dispatchers.IO) {
                                            paymentRepository.preparePaymentRedirect(receiptId, successUrl, canceledUrl, paymentStatus)
                                        }

                                        htmlResult.fold(
                                            onSuccess = { html ->
                                                // Trigger in-app WebView by storing HTML and setting trigger flag
                                                formData["paymentRedirectHtml"] = html
                                                formData["paymentRedirectSuccessUrl"] = successUrl
                                                formData["paymentRedirectCanceledUrl"] = canceledUrl
                                                formData["_triggerPaymentWebView"] = "true"

                                                println("🚀 Payment redirect prepared and WebView trigger set")
                                            },
                                            onFailure = { err ->
                                                println("⚠️ Failed to prepare payment redirect, falling back to showing success dialog: ${err.message}")
                                                // Fallback: show success dialog so user can see receipt id and retry later
                                                formData["showPaymentSuccessDialog"] = "true"
                                            }
                                        )
                                    } catch (e: Exception) {
                                        println("❌ Exception while preparing payment redirect: ${e.message}")
                                        formData["showPaymentSuccessDialog"] = "true"
                                    }

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
                    // ✅ NEW: Check if payment was already completed (after WebView success)
                    val paymentCompleted = formData["paymentCompleted"]?.toBoolean() ?: false

                    if (paymentCompleted) {
                        println("✅ Payment already completed - skipping payment details reload")
                        println("   User can now click 'Issue Certificate' button")
                        return StepProcessResult.Success("Payment completed - ready for certificate issuance")
                    }

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
                                println("   Final Total: ${receipt.finalTotal}")

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

                                // ✅ NEW: Check if finalTotal is 0 (free service)
                                if (receipt.finalTotal == 0.0) {
                                    println("💰 Final total is ZERO - This is a free service!")
                                    println("   Setting flags for manual certificate issuance via button")
                                    formData["isPaid"] = "1"
                                    formData["isFreeService"] = "true"
                                    formData["shouldIssueCertificate"] = "true"
                                    // Don't auto-issue - let user click button to issue
                                } else {
                                    println("💰 Final total is ${receipt.finalTotal} - Payment required")
                                    formData["isPaid"] = "0"
                                    formData["isFreeService"] = "false"
                                }

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

    /**
     * ✅ NEW: Issue certificate for free services or already paid requests
     * Called when finalTotal == 0 or isPaid == 1
     */
    private suspend fun issueCertificate(
        requestTypeId: Int,
        requestId: Int
    ): Result<String> {
        return try {
            println("🎫 PaymentManager: Issuing certificate...")
            println("   RequestTypeId: $requestTypeId")
            println("   RequestId: $requestId")

            // Get issuance endpoint from mapping
            val issuanceEndpoint = RequestTypeEndpoint.getIssuanceEndpoint(requestTypeId, requestId)

            if (issuanceEndpoint == null) {
                println("❌ PaymentManager: Issuance not supported for type ID: $requestTypeId")
                return Result.failure(Exception("إصدار الشهادة غير مدعوم لهذا النوع"))
            }

            println("📡 PaymentManager: Using issuance endpoint: $issuanceEndpoint")

            // Call issuance API
            val result = withContext(Dispatchers.IO) {
                userRequestsRepository.issueCertificate(issuanceEndpoint)
            }

            result.fold(
                onSuccess = { response ->
                    println("✅ PaymentManager: Certificate issued successfully")
                    println("📄 Response message: ${response.message}")

                    // Extract certificate URL from response
                    val certificateUrl = extractCertificateUrl(response, requestTypeId, requestId)

                    if (certificateUrl == null) {
                        println("❌ PaymentManager: Failed to extract certificate URL from response")
                        return@fold Result.failure(Exception("Failed to extract certificate URL"))
                    }

                    println("✅ Certificate URL: $certificateUrl")
                    Result.success(certificateUrl)
                },
                onFailure = { error ->
                    println("❌ PaymentManager: Error issuing certificate: ${error.message}")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            println("❌ Exception in issueCertificate: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * ✅ Extract certificate URL from response
     */
    private fun extractCertificateUrl(
        response: com.informatique.mtcit.data.model.requests.RequestDetailResponse,
        requestTypeId: Int,
        requestId: Int
    ): String? {
        return try {
            val dataObject = response.data.jsonObject

            // ✅ Extract certification number from response (structure varies by transaction type)
            val certNumber = when (requestTypeId) {
                4 -> {
                    // Mortgage Certificate: nested in mortgageCertification
                    val mortgageCert = dataObject["mortgageCertification"]?.jsonObject
                    mortgageCert?.get("certificationNumber")?.jsonPrimitive?.contentOrNull
                }
                5 -> {
                    // Mortgage Redemption: nested in mortgageRedemCertification (note: no "ption")
                    val redemptionCert = dataObject["mortgageRedemCertification"]?.jsonObject
                    redemptionCert?.get("certificationNumber")?.jsonPrimitive?.contentOrNull
                }
                7 -> {
                    // Cancel Registration: nested in deletionCertification
                    val deletionCert = dataObject["deletionCertification"]?.jsonObject
                    deletionCert?.get("certificationNumber")?.jsonPrimitive?.contentOrNull
                }
                else -> {
                    // Other types: direct field
                    dataObject["certificationNumber"]?.jsonPrimitive?.contentOrNull
                }
            }

            println("🔍 Extracted certificationNumber: $certNumber")

            if (certNumber != null) {
                // Build certificate URL using exact format from backend
                val baseUrl = "https://oman.isfpegypt.com/services"
                val certificateUrl = when (requestTypeId) {
                    1 -> "$baseUrl/temporary-registration/cert?certificateNumber=$certNumber&requestId=$requestId" // Temp Registration
                    2 -> "$baseUrl/permanent-registration/cert?certificateNumber=$certNumber&requestId=$requestId" // Permanent Registration
                    3 -> "$baseUrl/navigation-license/license-certificate?certificateNumber=$certNumber&requestId=$requestId" // Issue Navigation Permit
                    4 -> "$baseUrl/mortgage-certificate/cert?certificateNumber=$certNumber&requestId=$requestId" // Mortgage Certificate
                    5 -> "$baseUrl/mortgage-redemption/cert?certificateNumber=$certNumber&requestId=$requestId" // Release Mortgage
                    6 -> "$baseUrl/navigation-license-renewal/renewal-license-certificate?certificateNumber=$certNumber&requestId=$requestId" // Renew Navigation Permit
                    7 -> "$baseUrl/permanent-registration-cancellation/cert?certificateNumber=$certNumber&requestId=$requestId" // Cancel Permanent Registration
                    8 -> null // Request Inspection - No certificate issuance
                    else -> null
                }

                if (certificateUrl != null) {
                    println("✅ Built certificate URL: $certificateUrl")
                } else {
                    println("❌ Certificate URL not supported for requestTypeId: $requestTypeId")
                }
                certificateUrl
            } else {
                println("❌ Certificate number not found in response data")
                null
            }
        } catch (e: Exception) {
            println("❌ Error extracting certificate URL: ${e.message}")
            e.printStackTrace()
            null
        }
    }
}
