package com.informatique.mtcit.data.model

import com.informatique.mtcit.business.transactions.TransactionType
import com.informatique.mtcit.business.transactions.shared.MarineUnit
import com.informatique.mtcit.common.util.AppLanguage

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
            TransactionType.TEMPORARY_REGISTRATION_CERTIFICATE -> if (AppLanguage.isArabic) "شهادة التسجيل المؤقت" else "Temporary Registration Certificate"
            TransactionType.PERMANENT_REGISTRATION_CERTIFICATE -> if (AppLanguage.isArabic) "شهادة التسجيل الدائم" else "Permanent Registration Certificate"
            TransactionType.MORTGAGE_CERTIFICATE -> if (AppLanguage.isArabic) "شهادة الرهن" else "Mortgage Certificate"
            TransactionType.RELEASE_MORTGAGE -> if (AppLanguage.isArabic) "فك الرهن" else "Mortgage Release"
            TransactionType.SUSPEND_PERMANENT_REGISTRATION -> if (AppLanguage.isArabic) "إيقاف التسجيل الدائم" else "Suspend Permanent Registration"
            TransactionType.CANCEL_PERMANENT_REGISTRATION -> if (AppLanguage.isArabic) "إلغاء التسجيل الدائم" else "Cancel Permanent Registration"
            else -> if (AppLanguage.isArabic) "طلب" else "Request"
        }
    }

    /**
     * Get status display text in Arabic based on API status ID
     */
    fun getStatusText(): String {
        return when (statusId) {
            1 -> "مسودة"                    // Draft
            2 -> "مرفوض"                    // Rejected
            3 -> if (AppLanguage.isArabic) "مؤكد" else "Confirmed"                     // Confirmed
            4 -> "تم الإرسال"               // Sent
            5 -> "قيد الانتظار"             // Pending
            6 -> "مجدول"                    // Scheduled
            7 -> "مقبول"                    // Accepted
            8 -> if (AppLanguage.isArabic) "قيد المراجعة" else "Under Review"             // In Review
            9 -> if (AppLanguage.isArabic) "مراجعة RTA" else "RTA Review"               // RTA Review
            10 -> if (AppLanguage.isArabic) "رفض من الجهات" else "Auth. Rejected"          // Rejected by Authorities
            11 -> if (AppLanguage.isArabic) "موافقة الجهات" else "Auth. Approved"           // Approved by Authorities
            12 -> if (AppLanguage.isArabic) "الموافقة النهائية" else "Final Approval"      // Final Approval
            13 -> if (AppLanguage.isArabic) "تم اتخاذ الإجراء" else "Action Taken"        // Action Taken
            14 -> "مصدر"              // Issued
            15 -> if (AppLanguage.isArabic) "قيد التحقيق" else "Under Investigation"             // Under Investigation
            16 -> if (AppLanguage.isArabic) "في انتظار نتائج المعاينة" else "Waiting Inspection Results" // Waiting for Inspection
            else -> if (AppLanguage.isArabic) "غير معروف" else "Unknown"             // Unknown
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
