package com.informatique.mtcit.business.transactions

/**
 * ✅ Generic Transaction Context
 * Contains all API endpoints and configuration for any transaction type
 * This allows submitOnReview() to be completely generic across all transactions
 */
data class TransactionContext(
    /**
     * Transaction display name (e.g., "طلب رهن سفينة", "طلب فك رهن")
     */
    val displayName: String,

    /**
     * Transaction display name (e.g., "طلب رهن سفينة", "طلب فك رهن")
     */
    val sendRequestPostOrPut: String,

    /**
     * API endpoint for creating the initial request
     * Example: "mortgage-request/create-mortgage-request"
     */
    val createEndpoint: String,

    /**
     * API endpoint for updating request status
     * Uses {requestId} as placeholder
     * Example: "mortgage-request/{requestId}/update-status"
     */
    val updateStatusEndpoint: String,

    /**
     * API endpoint for final submission (send-request)
     * Uses {requestId} as placeholder
     * Example: "mortgage-request/{requestId}/send-request"
     */
    val sendRequestEndpoint: String,

    /**
     * API endpoint for getting request details
     * Uses {requestId} as placeholder
     * Example: "mortgage-request/{requestId}"
     */
    val getRequestEndpoint: String? = null,

    /**
     * API endpoint for deleting/canceling request
     * Uses {requestId} as placeholder
     * Example: "mortgage-request/{requestId}/delete"
     */
    val deleteRequestEndpoint: String? = null,

    /**
     * ✅ NEW: API endpoint for getting payment receipt
     * Example: "registration-requests/payment"
     */
    val paymentReceiptEndpoint: String? = null,

    /**
     * ✅ NEW: API endpoint for submitting payment
     * Example: "registration-requests/add-payment"
     */
    val paymentSubmitEndpoint: String? = null,

    /**
     * API endpoint for proceeding with a request based on shipInfoId
     * Uses {shipInfoId} as placeholder
     * Example: "mortgage-request/ship-info/{shipInfoId}/proceed-request"
     * This is called when user selects a ship to validate and proceed
     */
    val proceedRequestEndpoint: String? = null,

    /**
     * Additional transaction-specific configuration
     */
    val config: Map<String, Any> = emptyMap()
) {
    /**
     * Replace {requestId} placeholder with actual request ID
     */
    fun buildUpdateStatusUrl(requestId: Int): String {
        return updateStatusEndpoint.replace("{requestId}", requestId.toString())
    }

    fun buildSendRequestUrl(requestId: Int): String {
        return sendRequestEndpoint.replace("{requestId}", requestId.toString())
    }

    fun buildGetRequestUrl(requestId: Int): String? {
        return getRequestEndpoint?.replace("{requestId}", requestId.toString())
    }

    fun buildDeleteRequestUrl(requestId: Int): String? {
        return deleteRequestEndpoint?.replace("{requestId}", requestId.toString())
    }
}
