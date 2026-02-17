package com.informatique.mtcit.business.transactions.shared

/**
 * Data class representing a Certificate (شهادة)
 * Used in the Certificates list display
 */
data class Certificate(
    val id: String = "",                        // معرف فريد للشهادة
    val certificateNumber: String = "",         // رقم الشهادة (مثل: PR-2025-004512)
    val title: String = "",                     // عنوان الشهادة
    val issueDate: String = "",                 // تاريخ الإصدار
    val expiryDate: String = "",                // تاريخ الانتهاء
    val status: CertificateStatus = CertificateStatus.ACTIVE, // حالة الشهادة
    val certificateType: String = "",           // نوع الشهادة
    val issuingAuthority: String = "",          // الجهة المصدرة
    val remarks: String? = null                 // ملاحظات إضافية
) : java.io.Serializable {
    // Computed properties for UI display
    val isExpired: Boolean 
        get() = status == CertificateStatus.EXPIRED
    
    val isActive: Boolean 
        get() = status == CertificateStatus.ACTIVE
    
    val formattedIssueDate: String 
        get() = issueDate.ifEmpty { "-" }
    
    val formattedExpiryDate: String 
        get() = expiryDate.ifEmpty { "-" }
}

/**
 * Certificate status enum
 */
enum class CertificateStatus {
    ACTIVE,      // نشطة
    EXPIRED,     // منتهية
    SUSPENDED,   // معلقة
    CANCELLED    // ملغاة
}
