package com.informatique.mtcit.data.model

import java.util.Locale

/**
 * Request Status Enum
 * Maps API statusId to readable status names in both Arabic and English
 */
enum class RequestStatus(val statusId: Int, val arabicName: String, val englishName: String) {
    DRAFT(1, "مسودة", "Draft"),
    REJECTED(2, "مرفوض", "Rejected"),
    CONFIRMED(3, "مؤكد", "Confirmed"),
    SEND(4, "تم الإرسال", "Sent"),
    PENDING(5, "قيد الانتظار", "Pending"),
    SCHEDULED(6, "مجدول", "Scheduled"),
    ACCEPTED(7, "مقبول", "Accepted"),
    IN_REVIEW(8, "قيد المراجعة", "In Review"),
    REVIEW_RTA(9, "مراجعة RTA", "RTA Review"),
    REJECT_AUTHORITIES(10, "رفض من الجهات", "Rejected by Authorities"),
    APPROVED_AUTHORITIES(11, "موافقة الجهات", "Approved by Authorities"),
    APPROVED_FINAL(12, "الموافقة النهائية", "Final Approval"),
    ACTION_TAKEN(13, "تم اتخاذ الإجراء", "Action Taken"),
    ISSUED(14, "تم الإصدار", "Issued"),
    UNDER_INVESTIGATION(15, "قيد التحقيق", "Under Investigation"),
    WAITING_INSPECTION_RESULT(16, "في انتظار نتائج الفحص", "Waiting for Inspection Result");

    companion object {
        /**
         * Get status from API statusId
         */
        fun fromStatusId(statusId: Int): RequestStatus? {
            return values().find { it.statusId == statusId }
        }

        /**
         * Get localized status name based on current locale
         * @param statusId The status ID from the API
         * @return Localized status name (Arabic or English based on current locale)
         */
        fun getStatusName(statusId: Int): String {
            val status = fromStatusId(statusId)
            return if (status != null) {
                val currentLanguage = Locale.getDefault().language
                if (currentLanguage == "ar") status.arabicName else status.englishName
            } else {
                // Return "Unknown" in the appropriate language
                if (Locale.getDefault().language == "ar") "غير معروف" else "Unknown"
            }
        }

        /**
         * Get status name in specific language
         * @param statusId The status ID from the API
         * @param isArabic True for Arabic, false for English
         */
        fun getStatusName(statusId: Int, isArabic: Boolean): String {
            val status = fromStatusId(statusId)
            return if (status != null) {
                if (isArabic) status.arabicName else status.englishName
            } else {
                if (isArabic) "غير معروف" else "Unknown"
            }
        }
    }

    /**
     * Get localized name based on current locale
     */
    fun getLocalizedName(): String {
        val currentLanguage = Locale.getDefault().language
        return if (currentLanguage == "ar") arabicName else englishName
    }
}
