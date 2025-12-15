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
     * Get invoice type ID for a specific request
     */
    suspend fun getInvoiceTypeId(requestTypeId: String, requestId: Long): Result<Long>

    /**
     * Get payment details/calculation
     */
    suspend fun getPaymentDetails(requestTypeId: String): Result<PaymentReceipt>

    /**
     * Submit payment
     */
    suspend fun submitPayment(requestTypeId: String, paymentData: PaymentSubmissionRequest): Result<Long>

    /**
     * Get core ship data for payment submission
     */
    suspend fun getCoreShipData(requestTypeId: String, requestId: Long): Result<CoreShipsDto>
}

@Singleton
class PaymentRepositoryImpl @Inject constructor(
    private val apiService: PaymentApiService
) : PaymentRepository {

    override suspend fun getInvoiceTypeId(requestTypeId: String, requestId: Long): Result<Long> {
        return apiService.getInvoiceTypeId(requestTypeId, requestId)
    }

    override suspend fun getPaymentDetails(requestTypeId: String): Result<PaymentReceipt> {
        return apiService.getPaymentDetails(requestTypeId)
    }

    override suspend fun submitPayment(
        requestTypeId: String,
        paymentData: PaymentSubmissionRequest
    ): Result<Long> {
        return apiService.submitPayment(requestTypeId, paymentData)
    }

    override suspend fun getCoreShipData(requestTypeId: String, requestId: Long): Result<CoreShipsDto> {
        return apiService.getCoreShipData(requestTypeId, requestId)
    }
}
