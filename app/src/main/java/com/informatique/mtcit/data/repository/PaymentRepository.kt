package com.informatique.mtcit.data.repository

import com.informatique.mtcit.business.transactions.TransactionType
import com.informatique.mtcit.business.transactions.toRequestTypeId
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
     * Filter contains: {"requestType": 4, "coreShipsInfoId": "230"}
     */
    suspend fun getPaymentReceipt(requestType: Int, coreShipsInfoId: String): Result<PaymentReceipt>

    /**
     * Submit payment
     */
    suspend fun submitPayment(requestTypeId: String, paymentData: PaymentSubmissionRequest): Result<Long>
}

@Singleton
class PaymentRepositoryImpl @Inject constructor(
    private val apiService: PaymentApiService
) : PaymentRepository {

    override suspend fun getPaymentReceipt(requestType: Int, coreShipsInfoId: String): Result<PaymentReceipt> {
        return apiService.getPaymentReceipt(requestType, coreShipsInfoId)
    }

    override suspend fun submitPayment(
        requestTypeId: String,
        paymentData: PaymentSubmissionRequest
    ): Result<Long> {
        return apiService.submitPayment(requestTypeId, paymentData)
    }
}
