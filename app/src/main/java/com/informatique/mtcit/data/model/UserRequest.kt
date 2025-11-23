package com.informatique.mtcit.data.model

import com.informatique.mtcit.business.transactions.TransactionType
import com.informatique.mtcit.business.transactions.shared.MarineUnit

/**
 * Represents a user's transaction request saved in their profile (الاستمارات)
 *
 * This model is used for:
 * 1. Showing requests in user profile
 * 2. Saving transaction progress when inspection is PENDING
 * 3. Resuming transactions when inspection becomes VERIFIED
 *
 * API Mapping:
 * - GET /api/users/{userId}/requests → List<UserRequest>
 * - GET /api/requests/{requestId}/status → UserRequest
 * - POST /api/requests/save → Save request progress
 */
data class UserRequest(
    val id: String,                           // Unique request ID (from backend)
    val userId: String,                       // User who created the request
    val type: TransactionType,                // Type of transaction
    val status: RequestStatus,                // Current status
    val marineUnit: MarineUnit?,              // Marine unit associated with request
    val createdDate: String,                  // When request was created (ISO 8601)
    val lastUpdatedDate: String,              // Last status update (ISO 8601)
    val formData: Map<String, String>,        // All form data collected so far
    val lastCompletedStep: Int,               // Last step user completed (0-based index)
    val rejectionReason: String? = null,      // If rejected, why?
    val inspectionCertificateUrl: String? = null, // URL to inspection certificate (if verified)
    val estimatedCompletionDate: String? = null   // Expected date for pending requests
) {
    /**
     * Get display title in Arabic based on transaction type
     */
    fun getDisplayTitle(): String {
        return when (type) {
            TransactionType.TEMPORARY_REGISTRATION_CERTIFICATE -> "شهادة التسجيل المؤقت"
            TransactionType.PERMANENT_REGISTRATION_CERTIFICATE -> "شهادة التسجيل الدائم"
            TransactionType.MORTGAGE_CERTIFICATE -> "شهادة الرهن"
            TransactionType.RELEASE_MORTGAGE -> "فك الرهن"
            TransactionType.SUSPEND_PERMANENT_REGISTRATION -> "إيقاف التسجيل الدائم"
            TransactionType.CANCEL_PERMANENT_REGISTRATION -> "إلغاء التسجيل الدائم"
            else -> "طلب"
        }
    }

    /**
     * Get status display text in Arabic
     */
    fun getStatusText(): String {
        return when (status) {
            RequestStatus.PENDING -> "قيد المراجعة"
            RequestStatus.VERIFIED -> "تم التحقق"
            RequestStatus.REJECTED -> "مرفوض"
            RequestStatus.IN_PROGRESS -> "قيد المعالجة"
            RequestStatus.COMPLETED -> "مكتمل"
        }
    }

    /**
     * Check if request can be resumed (verified and not completed)
     */
    fun canResume(): Boolean {
        return status == RequestStatus.VERIFIED
    }

    /**
     * Check if request is still waiting for review
     */
    fun isPending(): Boolean {
        return status == RequestStatus.PENDING || status == RequestStatus.IN_PROGRESS
    }
}

/**
 * Status of a user request
 *
 * Flow:
 * 1. User submits → PENDING (waiting for inspection)
 * 2. Inspection done → VERIFIED (can continue transaction)
 * 3. Inspection failed → REJECTED (cannot continue)
 * 4. User completes transaction → COMPLETED
 */
enum class RequestStatus {
    PENDING,        // Waiting for inspection/approval (قيد المراجعة)
    IN_PROGRESS,    // Being processed by admin (قيد المعالجة)
    VERIFIED,       // Inspection verified - user can resume (تم التحقق)
    REJECTED,       // Inspection rejected - cannot continue (مرفوض)
    COMPLETED       // Transaction fully completed (مكتمل)
}

/**
 * API Response wrapper for user requests
 * Used when calling GET /api/users/{userId}/requests
 */
data class UserRequestsResponse(
    val success: Boolean,
    val requests: List<UserRequest>,
    val totalCount: Int,
    val errorMessage: String? = null
)

/**
 * API Request body for saving request progress
 * Used when calling POST /api/requests/save
 */
data class SaveRequestBody(
    val userId: String,
    val transactionType: TransactionType,
    val marineUnitId: String,
    val formData: Map<String, String>,
    val lastCompletedStep: Int
)

