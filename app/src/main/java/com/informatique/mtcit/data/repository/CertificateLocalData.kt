package com.informatique.mtcit.data.repository

import com.informatique.mtcit.business.transactions.shared.Certificate
import com.informatique.mtcit.business.transactions.shared.CertificateStatus
import com.informatique.mtcit.common.util.AppLanguage

/**
 * بيانات تجريبية للشهادات - للاختبار السريع
 * لاحقاً هتستبدلها بـ API call
 */
object CertificateLocalData {

    /**
     * بيانات تجريبية تطابق الصورة اللي بعتها
     */
    fun getSampleCertificates(): List<Certificate> {
        return listOf(
            Certificate(
                id = "1",
                certificateNumber = "PR-2025-004512",
                title = if (AppLanguage.isArabic) "شطب شهادة تسجيل دائمة للسفن والوحدات البحرية" else "Cancellation of Permanent Registration Certificate for Ships and Marine Units",
                issueDate = "14-02-2025",
                expiryDate = "13-02-2026",
                status = CertificateStatus.ACTIVE,
                certificateType = if (AppLanguage.isArabic) "تسجيل دائم" else "Permanent Registration",
                issuingAuthority = if (AppLanguage.isArabic) "الهيئة العامة للنقل البحري" else "General Authority for Maritime Transport"
            ),
            Certificate(
                id = "2",
                certificateNumber = "PR-2025-004512",
                title = if (AppLanguage.isArabic) "شطب شهادة تسجيل دائمة للسفن والوحدات البحرية" else "Cancellation of Permanent Registration Certificate for Ships and Marine Units",
                issueDate = "14-02-2025",
                expiryDate = "13-02-2026",
                status = CertificateStatus.ACTIVE,
                certificateType = if (AppLanguage.isArabic) "تسجيل دائم" else "Permanent Registration",
                issuingAuthority = if (AppLanguage.isArabic) "الهيئة العامة للنقل البحري" else "General Authority for Maritime Transport"
            ),
            Certificate(
                id = "3",
                certificateNumber = "PR-2025-004512",
                title = if (AppLanguage.isArabic) "شطب شهادة تسجيل دائمة للسفن والوحدات البحرية" else "Cancellation of Permanent Registration Certificate for Ships and Marine Units",
                issueDate = "14-02-2025",
                expiryDate = "13-02-2026",
                status = CertificateStatus.ACTIVE,
                certificateType = if (AppLanguage.isArabic) "تسجيل دائم" else "Permanent Registration",
                issuingAuthority = if (AppLanguage.isArabic) "الهيئة العامة للنقل البحري" else "General Authority for Maritime Transport"
            )
        )
    }

    /**
     * محاكاة API call
     */
    suspend fun fetchCertificatesFromApi(): Result<List<Certificate>> {
        return try {
            kotlinx.coroutines.delay(500) // Simulate network delay
            Result.success(getSampleCertificates())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
