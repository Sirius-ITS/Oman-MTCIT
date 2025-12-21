package com.informatique.mtcit.data.api

import com.informatique.mtcit.data.model.*
import com.informatique.mtcit.di.module.AppRepository
import com.informatique.mtcit.di.module.RepoServiceState
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.int
import android.util.Base64
import kotlinx.serialization.json.decodeFromJsonElement
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
     * Get payment receipt with base64 encoded filter
     * GET /{endpoint}/payment?filter={base64}
     * Filter: {"requestType": 1, "requestId": 1026, "coreShipsInfoId": "1184"}
     *
     * ✅ Generic - works for any transaction type
     */
    suspend fun getPaymentReceipt(
        endpoint: String,
        requestType: Int,
        requestId: String,
        coreShipsInfoId: String
    ): Result<PaymentReceipt> {
        return try {
            println("🚀 PaymentApiService: Getting payment receipt...")
            println("   Endpoint: $endpoint")
            println("   RequestType: $requestType, RequestId: $requestId, CoreShipsInfoId: $coreShipsInfoId")

            // ✅ Create filter with all three parameters
            val filterJson = kotlinx.serialization.json.buildJsonObject {
                put("requestType", kotlinx.serialization.json.JsonPrimitive(requestType))
                put("requestId", kotlinx.serialization.json.JsonPrimitive(requestId.toInt()))
                put("coreShipsInfoId", kotlinx.serialization.json.JsonPrimitive(coreShipsInfoId))
            }.toString()

            println("📤 Filter JSON: $filterJson")

            // Encode to base64
            val base64Filter = Base64.encodeToString(filterJson.toByteArray(), Base64.NO_WRAP)
            println("📤 Base64 Filter: $base64Filter")

            // ✅ Use dynamic endpoint (not hardcoded)
            val fullUrl = "$endpoint?filter=$base64Filter"
            println("📡 Full URL: $fullUrl")

            when (val response = repo.onGet(fullUrl)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ API Response received")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int

                        if (statusCode == 200) {
                            // Parse the payment receipt
                            val paymentResponse: PaymentResponse<PaymentReceipt> = json.decodeFromJsonElement(
                                PaymentResponse.serializer(PaymentReceipt.serializer()),
                                responseJson
                            )
                            println("✅ Payment receipt retrieved successfully!")
                            println("   Total Cost: ${paymentResponse.data.totalCost}")
                            println("   Total Tax: ${paymentResponse.data.totalTax}")
                            println("   Final Total: ${paymentResponse.data.finalTotal}")
                            println("   Receipt Serial: ${paymentResponse.data.receiptSerial}/${paymentResponse.data.receiptYear}")
                            Result.success(paymentResponse.data)
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                ?: "Failed to get payment receipt"
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
            println("❌ Exception in getPaymentReceipt: ${e.message}")
            e.printStackTrace()
            Result.failure(Exception("Failed to get payment receipt: ${e.message}"))
        }
    }

    /**
     * Submit payment
     * POST /{endpoint}/add-payment
     *
     * ✅ Generic - works for any transaction type
     */
    suspend fun submitPayment(
        endpoint: String,
        requestTypeId: String,
        paymentData: PaymentSubmissionRequest
    ): Result<Long> {
        return try {
            println("🚀 PaymentApiService: Submitting payment...")
            println("   Endpoint: $endpoint")
            println("   RequestTypeId: $requestTypeId")
            println("   Final Total: ${paymentData.finalTotal}")

            val requestBody = json.encodeToString(PaymentSubmissionRequest.serializer(), paymentData)
            println("📤 Request Body: $requestBody")

            when (val response = repo.onPostAuth(endpoint, requestBody)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ API Response received")

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
     * ✅ NEW: Simple payment submission - used when user clicks Pay button
     * POST /{endpoint}
     * Request body: {"requestType": 2, "requestId": 9, "coreShipsInfoId": "33"}
     *
     * Response: {
     *   "message": "تم الاضافة بنجاح",
     *   "statusCode": 200,
     *   "success": true,
     *   "timestamp": "2025-12-14 14:31:19",
     *   "data": 102
     * }
     */
    suspend fun submitSimplePayment(
        endpoint: String,
        requestType: Int,
        requestId: Int,
        coreShipsInfoId: String
    ): Result<PaymentResponse<Long>> {
        return try {
            println("🚀 PaymentApiService: Submitting simple payment...")
            println("   Endpoint: $endpoint")
            println("   RequestType: $requestType, RequestId: $requestId, CoreShipsInfoId: $coreShipsInfoId")

            val paymentRequest = SimplePaymentRequest(
                requestType = requestType,
                requestId = requestId,
                coreShipsInfoId = coreShipsInfoId
            )

            val requestBody = json.encodeToString(SimplePaymentRequest.serializer(), paymentRequest)
            println("📤 Request Body: $requestBody")

            when (val response = repo.onPostAuth(endpoint, requestBody)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ API Response received")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int

                        if (statusCode == 200 || statusCode == 201) {
                            val paymentResponse: PaymentResponse<Long> = json.decodeFromJsonElement(responseJson)
                            println("✅ Payment submitted successfully!")
                            println("   Receipt ID: ${paymentResponse.data}")
                            println("   Message: ${paymentResponse.message}")
                            println("   Timestamp: ${paymentResponse.timestamp}")
                            Result.success(paymentResponse)
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
            println("❌ Exception in submitSimplePayment: ${e.message}")
            e.printStackTrace()
            Result.failure(Exception("Failed to submit payment: ${e.message}"))
        }
    }
}
