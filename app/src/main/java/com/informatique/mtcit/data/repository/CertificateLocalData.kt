package com.informatique.mtcit.data.repository

import com.informatique.mtcit.business.transactions.shared.Certificate
import com.informatique.mtcit.business.transactions.shared.CertificateStatus

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
                title = "شطب شهادة تسجيل دائمة للسفن والوحدات البحرية",
                issueDate = "14-02-2025",
                expiryDate = "13-02-2026",
                status = CertificateStatus.ACTIVE,
                certificateType = "تسجيل دائم",
                issuingAuthority = "الهيئة العامة للنقل البحري"
            ),
            Certificate(
                id = "2",
                certificateNumber = "PR-2025-004512",
                title = "شطب شهادة تسجيل دائمة للسفن والوحدات البحرية",
                issueDate = "14-02-2025",
                expiryDate = "13-02-2026",
                status = CertificateStatus.ACTIVE,
                certificateType = "تسجيل دائم",
                issuingAuthority = "الهيئة العامة للنقل البحري"
            ),
            Certificate(
                id = "3",
                certificateNumber = "PR-2025-004512",
                title = "شطب شهادة تسجيل دائمة للسفن والوحدات البحرية",
                issueDate = "14-02-2025",
                expiryDate = "13-02-2026",
                status = CertificateStatus.ACTIVE,
                certificateType = "تسجيل دائم",
                issuingAuthority = "الهيئة العامة للنقل البحري"
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
