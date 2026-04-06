package com.informatique.mtcit.business.transactions.marineunit

import com.informatique.mtcit.business.transactions.shared.MarineUnit
import com.informatique.mtcit.common.util.AppLanguage

/**
 * Sealed class representing the result of marine unit validation
 * for transaction eligibility
 */
sealed class MarineUnitValidationResult {
    /**
     * Unit is eligible for the transaction
     */
    data class Eligible(
        val unit: MarineUnit,
        val additionalData: Map<String, Any> = emptyMap()
    ) : MarineUnitValidationResult()

    /**
     * Unit is not eligible - contains reason and suggestion
     */
    sealed class Ineligible : MarineUnitValidationResult() {
        abstract val unit: MarineUnit
        abstract val reason: String
        abstract val suggestion: String?

        data class NotOwned(
            override val unit: MarineUnit,
            val actualOwner: String? = null
        ) : Ineligible() {
            override val reason = if (AppLanguage.isArabic) "الوحدة البحرية غير مسجلة باسمك" else "Marine unit is not registered in your name"
            override val suggestion = if (AppLanguage.isArabic) "يرجى اختيار وحدة بحرية مملوكة لك" else "Please select a marine unit owned by you"
        }

        data class AlreadyMortgaged(
            override val unit: MarineUnit,
            val bankName: String,
            val mortgageEndDate: String
        ) : Ineligible() {
            override val reason = if (AppLanguage.isArabic) "الوحدة البحرية مرهونة بالفعل لدى $bankName" else "Marine unit is already mortgaged to $bankName"
            override val suggestion = if (AppLanguage.isArabic) "يمكنك تقديم طلب فك الرهن أولاً" else "You can submit a mortgage release request first"
        }

        data class NotMortgaged(
            override val unit: MarineUnit
        ) : Ineligible() {
            override val reason = if (AppLanguage.isArabic) "الوحدة البحرية غير مرهونة" else "Marine unit is not mortgaged"
            override val suggestion = if (AppLanguage.isArabic) "هذه المعاملة لفك الرهن فقط. اختر وحدة مرهونة" else "This transaction is for mortgage release only. Select a mortgaged unit"
        }

        data class TemporaryRegistration(
            override val unit: MarineUnit
        ) : Ineligible() {
            override val reason = if (AppLanguage.isArabic) "الوحدة لديها شهادة تسجيل مؤقتة فقط" else "Unit only has a temporary registration certificate"
            override val suggestion = if (AppLanguage.isArabic) "يجب الحصول على شهادة تسجيل دائمة أولاً" else "Must obtain a permanent registration certificate first"
        }

        data class SuspendedOrCancelled(
            override val unit: MarineUnit,
            val status: String
        ) : Ineligible() {
            override val reason = if (AppLanguage.isArabic) "حالة التسجيل: $status" else "Registration status: $status"
            override val suggestion = if (AppLanguage.isArabic) "لا يمكن إجراء هذه المعاملة على وحدة متوقفة أو ملغاة" else "Cannot perform this transaction on a suspended or cancelled unit"
        }

        data class HasViolations(
            override val unit: MarineUnit,
            val violationsCount: Int
        ) : Ineligible() {
            override val reason = if (AppLanguage.isArabic) "لا يمكن رهن وحدة بحرية لديها مخالفات نشطة ($violationsCount مخالفة)" else "Cannot mortgage a unit with active violations ($violationsCount violations)"
            override val suggestion = if (AppLanguage.isArabic) "يجب تسوية المخالفات أولاً قبل تقديم طلب الرهن" else "Must settle violations first before submitting a mortgage request"
        }

        data class HasDetentions(
            override val unit: MarineUnit,
            val detentionsCount: Int
        ) : Ineligible() {
            override val reason = if (AppLanguage.isArabic) "لا يمكن رهن وحدة بحرية محتجزة ($detentionsCount احتجاز)" else "Cannot mortgage a detained unit ($detentionsCount detentions)"
            override val suggestion = if (AppLanguage.isArabic) "يجب فك الاحتجاز أولاً قبل تقديم طلب الرهن" else "Must release detention first before submitting a mortgage request"
        }

        data class CustomError(
            override val unit: MarineUnit,
            override val reason: String,
            override val suggestion: String? = null
        ) : Ineligible()
    }
}
