package com.informatique.mtcit.data.repository

import com.informatique.mtcit.data.api.PaymentApiService
import com.informatique.mtcit.data.model.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for payment-related operations
 * Handles all payment API calls across different transaction types
 */
interface PaymentRepository {
    /**
     * Get payment receipt/details with base64 encoded filter
     * ✅ Now includes endpoint and requestId
     */
    suspend fun getPaymentReceipt(
        endpoint: String,
        requestType: Int,
        requestId: String,
        coreShipsInfoId: String
    ): Result<PaymentReceipt>

    /**
     * ✅ NEW: Submit simple payment (addPayment API)
     * Used when user clicks Pay button on payment details step
     */
    suspend fun submitPayment(
        endpoint: String,
        requestType: Int,
        requestId: Int,
        coreShipsInfoId: String,
    ): Result<PaymentResponse<Long>>
}

@Singleton
class PaymentRepositoryImpl @Inject constructor(
    private val apiService: PaymentApiService
) : PaymentRepository {

    override suspend fun getPaymentReceipt(
        endpoint: String,
        requestType: Int,
        requestId: String,
        coreShipsInfoId: String
    ): Result<PaymentReceipt> {
        return apiService.getPaymentReceipt(endpoint, requestType, requestId, coreShipsInfoId)
    }

    override suspend fun submitPayment(
        endpoint: String,
        requestType: Int,
        requestId: Int,
        coreShipsInfoId: String
    ): Result<PaymentResponse<Long>> {
        return apiService.submitSimplePayment(endpoint, requestType, requestId, coreShipsInfoId)
    }
}
