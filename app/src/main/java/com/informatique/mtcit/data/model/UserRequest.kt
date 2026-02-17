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
    val statusId: Int,                        // Current status ID from API (1-16)
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
     * Get status display text in Arabic based on API status ID
     */
    fun getStatusText(): String {
        return when (statusId) {
            1 -> "مسودة"                    // Draft
            2 -> "مرفوض"                    // Rejected
            3 -> "مؤكد"                     // Confirmed
            4 -> "تم الإرسال"               // Sent
            5 -> "قيد الانتظار"             // Pending
            6 -> "مجدول"                    // Scheduled
            7 -> "مقبول"                    // Accepted
            8 -> "قيد المراجعة"             // In Review
            9 -> "مراجعة RTA"               // RTA Review
            10 -> "رفض من الجهات"          // Rejected by Authorities
            11 -> "موافقة الجهات"           // Approved by Authorities
            12 -> "الموافقة النهائية"      // Final Approval
            13 -> "تم اتخاذ الإجراء"        // Action Taken
            14 -> "تم الإصدار"              // Issued
            15 -> "قيد التحقيق"             // Under Investigation
            16 -> "في انتظار نتائج المعاينة" // Waiting for Inspection
            else -> "غير معروف"             // Unknown
        }
    }

    /**
     * Check if request can be resumed (accepted and ready for payment)
     */
    fun canResume(): Boolean {
        return statusId == 7 // Accepted - ready for payment
    }

    /**
     * Check if request is still waiting for review
     */
    fun isPending(): Boolean {
        return statusId in listOf(4, 5, 6, 8, 9, 15, 16) // Sent, Pending, Scheduled, In Review, RTA Review, Under Investigation, Waiting for Inspection
    }

    /**
     * Check if request is rejected
     */
    fun isRejected(): Boolean {
        return statusId in listOf(2, 10) // Rejected, Rejected by Authorities
    }

    /**
     * Check if request is completed
     */
    fun isCompleted(): Boolean {
        return statusId in listOf(13, 14) // Action Taken, Issued
    }
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
