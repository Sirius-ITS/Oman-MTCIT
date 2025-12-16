package com.informatique.mtcit.data.api

import com.informatique.mtcit.data.model.*
import com.informatique.mtcit.di.module.AppRepository
import com.informatique.mtcit.di.module.RepoServiceState
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.int
import android.util.Base64
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
     * GET /api/v1/registration-requests/payment?filter={base64}
     * Filter: {"requestType": 4, "coreShipsInfoId": "230"}
     */
    suspend fun getPaymentReceipt(requestType: Int, coreShipsInfoId: String): Result<PaymentReceipt> {
        return try {
            println("🚀 PaymentApiService: Getting payment receipt...")
            println("   RequestType: $requestType, CoreShipsInfoId: $coreShipsInfoId")

            // Create the filter JSON
            val filterRequest = PaymentReceiptRequest(
                requestType = requestType,
                coreShipsInfoId = coreShipsInfoId
            )
            val filterJson = json.encodeToString(PaymentReceiptRequest.serializer(), filterRequest)
            println("📤 Filter JSON: $filterJson")

            // Encode to base64
            val base64Filter = Base64.encodeToString(filterJson.toByteArray(), Base64.NO_WRAP)
            println("📤 Base64 Filter: $base64Filter")

            // Call API with base64 filter
            when (val response = repo.onGet("api/v1/registration-requests/payment?filter=$base64Filter")) {
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
     * POST /api/v1/payment-receipt/add
     */
    suspend fun submitPayment(requestTypeId: String, paymentData: PaymentSubmissionRequest): Result<Long> {
        return try {
            println("🚀 PaymentApiService: Submitting payment...")
            println("   RequestTypeId: $requestTypeId")
            println("   Final Total: ${paymentData.finalTotal}")

            val requestBody = json.encodeToString(PaymentSubmissionRequest.serializer(), paymentData)
            println("📤 Request Body: $requestBody")

            when (val response = repo.onPostAuth("api/v1/payment-receipt/add", requestBody)) {
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
}
