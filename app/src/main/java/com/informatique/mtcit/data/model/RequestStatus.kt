package com.informatique.mtcit.data.model

import com.informatique.mtcit.common.util.AppLanguage

/**
 * Request Status Enum
 * Maps API statusId to readable status names in both Arabic and English.
 * arabicName and englishName are always the literal strings — never conditional —
 * so reactive display in Composables (using LocalAppLocale.current) works correctly
 * regardless of what language the app was in when the enum was first loaded.
 */
enum class RequestStatus(val statusId: Int, val arabicName: String, val englishName: String) {
    DRAFT(1, "مسودة", "Draft"),
    REJECTED(2, "مرفوض", "Rejected"),
    CONFIRMED(3, "مؤكد", "Confirmed"),
    SEND(4, "تم الإرسال", "Sent"),
    PENDING(5, "قيد الانتظار", "Pending"),
    SCHEDULED(6, "مجدول", "Scheduled"),
    ACCEPTED(7, "مقبول", "Accepted"),
    IN_REVIEW(8, "قيد المراجعة", "Under Review"),
    REVIEW_RTA(9, "مراجعة RTA", "RTA Review"),
    REJECT_AUTHORITIES(10, "رفض من الجهات", "Auth. Rejected"),
    APPROVED_AUTHORITIES(11, "موافقة الجهات", "Auth. Approved"),
    APPROVED_FINAL(12, "الموافقة النهائية", "Final Approval"),
    ACTION_TAKEN(13, "تم اتخاذ الإجراء", "Action Taken"),
    ISSUED(14, "مصدر", "Issued"),
    UNDER_INVESTIGATION(15, "قيد التحقيق", "Under Investigation"),
    WAITING_INSPECTION_RESULT(16, "في انتظار نتائج الفحص", "Waiting Inspection");

    companion object {
        /**
         * Get status from API statusId
         */
        fun fromStatusId(statusId: Int): RequestStatus? {
            return entries.find { it.statusId == statusId }
        }

        /**
         * Get localized status name based on current default locale.
         * NOTE: Call this only from non-Composable code.
         * In Composables use: RequestStatus.fromStatusId(id)?.let { if (isAr) it.arabicName else it.englishName }
         */
        fun getStatusName(statusId: Int): String {
            val status = fromStatusId(statusId) ?: return if (AppLanguage.isArabic) "غير معروف" else "Unknown"
            return if (AppLanguage.isArabic) status.arabicName else status.englishName
        }

        /**
         * Get status name in a specific language
         */
        fun getStatusName(statusId: Int, isArabic: Boolean): String {
            val status = fromStatusId(statusId) ?: return if (isArabic) "غير معروف" else "Unknown"
            return if (isArabic) status.arabicName else status.englishName
        }
    }

    /**
     * Get localized name based on current default locale.
     * For reactive Composable display, read arabicName / englishName directly with LocalAppLocale.current.
     */
    fun getLocalizedName(): String {
        return if (AppLanguage.isArabic) arabicName else englishName
    }
}
