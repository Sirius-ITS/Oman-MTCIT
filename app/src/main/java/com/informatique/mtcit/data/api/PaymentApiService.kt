package com.informatique.mtcit.data.api

import com.informatique.mtcit.data.model.*
import com.informatique.mtcit.di.module.AppRepository
import com.informatique.mtcit.di.module.RepoServiceState
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.int
import javax.inject.Inject
import javax.inject.Singleton

/**
 * API Service for Payment operations
 * Follows the same pattern as RegistrationApiService
 */
@Singleton
class PaymentApiService @Inject constructor(
    private val repo: AppRepository,
    private val json: Json
) {

    /**
     * Get invoice type ID for a request
     * GET /api/v1/reqtype/{requestTypeId}/{requestId}
     */
    suspend fun getInvoiceTypeId(requestTypeId: String, requestId: Long): Result<Long> {
        return try {
            println("🚀 PaymentApiService: Getting invoice type ID...")
            println("   RequestTypeId: $requestTypeId, RequestId: $requestId")

            when (val response = repo.onGet("api/v1/reqtype/$requestTypeId/$requestId")) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ API Response received: $responseJson")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int

                        if (statusCode == 200) {
                            val invoiceTypeId = responseJson.jsonObject.getValue("data").jsonPrimitive.content.toLong()
                            println("✅ Invoice Type ID retrieved: $invoiceTypeId")
                            Result.success(invoiceTypeId)
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                ?: "Failed to get invoice type ID"
                            println("❌ API returned error: $message (Status: $statusCode)")
                            Result.failure(Exception(message))
                        }
                    } else {
                        println("❌ Empty response from API")
                        Result.failure(Exception("Empty response from server"))
                    }
                }
                is RepoServiceState.Error -> {
                    println("❌ API Error: ${response.error}")
                    Result.failure(Exception("API Error ${response.code}: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in getInvoiceTypeId: ${e.message}")
            e.printStackTrace()
            Result.failure(Exception("Failed to get invoice type ID: ${e.message}"))
        }
    }

    /**
     * Get core ship data for payment
     * GET /api/v1/reqtype/{requestTypeId}/{requestId}
     */
    suspend fun getCoreShipData(requestTypeId: String, requestId: Long): Result<CoreShipsDto> {
        return try {
            println("🚀 PaymentApiService: Getting core ship data...")
            println("   RequestTypeId: $requestTypeId, RequestId: $requestId")

            when (val response = repo.onGet("api/v1/reqtype/$requestTypeId/$requestId")) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ API Response received: $responseJson")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int

                        if (statusCode == 200) {
                            // Parse the core ship data from response
                            val coreShipData: PaymentResponse<CoreShipsDto> = json.decodeFromJsonElement(
                                PaymentResponse.serializer(CoreShipsDto.serializer()),
                                responseJson
                            )
                            println("✅ Core ship data retrieved: ${coreShipData.data.shipName} (ID: ${coreShipData.data.id})")
                            Result.success(coreShipData.data)
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                ?: "Failed to get core ship data"
                            println("❌ API returned error: $message (Status: $statusCode)")
                            Result.failure(Exception(message))
                        }
                    } else {
                        println("❌ Empty response from API")
                        Result.failure(Exception("Empty response from server"))
                    }
                }
                is RepoServiceState.Error -> {
                    println("❌ API Error: ${response.error}")
                    Result.failure(Exception("API Error ${response.code}: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in getCoreShipData: ${e.message}")
            e.printStackTrace()
            Result.failure(Exception("Failed to get core ship data: ${e.message}"))
        }
    }

    /**
     * Get payment details - GENERIC method that maps request type to endpoint
     * GET /api/v1/{mapped-endpoint}/payment
     */
    suspend fun getPaymentDetails(requestTypeId: String): Result<PaymentReceipt> {
        return try {
            val endpoint = mapRequestTypeToPaymentEndpoint(requestTypeId)
            println("🚀 PaymentApiService: Getting payment details...")
            println("   RequestTypeId: $requestTypeId → Endpoint: $endpoint")

            when (val response = repo.onGet("api/v1/$endpoint/payment")) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ API Response received: $responseJson")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int

                        if (statusCode == 200) {
                            // Parse the payment receipt
                            val paymentResponse: PaymentResponse<PaymentReceipt> = json.decodeFromJsonElement(
                                PaymentResponse.serializer(PaymentReceipt.serializer()),
                                responseJson
                            )
                            println("✅ Payment details retrieved successfully!")
                            println("   Total Cost: ${paymentResponse.data.totalCost}")
                            println("   Total Tax: ${paymentResponse.data.totalTax}")
                            println("   Final Total: ${paymentResponse.data.finalTotal}")
                            println("   Receipt Serial: ${paymentResponse.data.receiptSerial}/${paymentResponse.data.receiptYear}")
                            Result.success(paymentResponse.data)
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                ?: "Failed to get payment details"
                            println("❌ API returned error: $message (Status: $statusCode)")
                            Result.failure(Exception(message))
                        }
                    } else {
                        println("❌ Empty response from API")
                        Result.failure(Exception("Empty response from server"))
                    }
                }
                is RepoServiceState.Error -> {
                    println("❌ API Error: ${response.error}")
                    Result.failure(Exception("API Error ${response.code}: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in getPaymentDetails: ${e.message}")
            e.printStackTrace()
            Result.failure(Exception("Failed to get payment details: ${e.message}"))
        }
    }

    /**
     * Submit payment - GENERIC method that maps request type to endpoint
     * POST /api/v1/{mapped-endpoint}/add-payment
     */
    suspend fun submitPayment(requestTypeId: String, paymentData: PaymentSubmissionRequest): Result<Long> {
        return try {
            val endpoint = mapRequestTypeToPaymentEndpoint(requestTypeId)
            println("🚀 PaymentApiService: Submitting payment...")
            println("   RequestTypeId: $requestTypeId → Endpoint: $endpoint")
            println("   Request ID: ${paymentData.requestId}")
            println("   Final Total: ${paymentData.finalTotal}")

            val requestBody = json.encodeToString(PaymentSubmissionRequest.serializer(), paymentData)
            println("📤 Request Body: $requestBody")

            when (val response = repo.onPostAuth("api/v1/$endpoint/add-payment", requestBody)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ API Response received: $responseJson")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int

                        if (statusCode == 200 || statusCode == 201) {
                            val paymentReceiptId = responseJson.jsonObject.getValue("data").jsonPrimitive.content.toLong()
                            println("✅ Payment submitted successfully! Receipt ID: $paymentReceiptId")
                            Result.success(paymentReceiptId)
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                ?: "Failed to submit payment"
                            println("❌ API returned error: $message (Status: $statusCode)")
                            Result.failure(Exception(message))
                        }
                    } else {
                        println("❌ Empty response from API")
                        Result.failure(Exception("Empty response from server"))
                    }
                }
                is RepoServiceState.Error -> {
                    println("❌ API Error: ${response.error}")
                    Result.failure(Exception("API Error ${response.code}: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in submitPayment: ${e.message}")
            e.printStackTrace()
            Result.failure(Exception("Failed to submit payment: ${e.message}"))
        }
    }

    /**
     * Map request type ID to the corresponding payment endpoint
     * This centralizes the endpoint mapping logic
     */
    private fun mapRequestTypeToPaymentEndpoint(requestTypeId: String): String {
        return when (requestTypeId) {
            "1" -> "registration-requests"           // Temporary Registration
            "2" -> "registration-requests"           // Permanent Registration
            "3" -> "ship-navigation-license-request" // Issue Navigation License
            "4" -> "mortgage-request"                // Mortgage Certificate
            "5" -> "mortgage-redemption-request"     // Release Mortgage
            "6" -> "navigation-license-renewal-request" // Renew Navigation License
            "7" -> "deletion-requests"               // Cancel Registration
            "8" -> "inspection-requests"             // Request Inspection
            else -> {
                println("⚠️ Unknown request type ID: $requestTypeId, defaulting to 'registration-requests'")
                "registration-requests"
            }
        }
    }
}
