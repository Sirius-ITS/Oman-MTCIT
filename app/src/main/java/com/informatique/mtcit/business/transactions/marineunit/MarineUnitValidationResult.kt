package com.informatique.mtcit.business.transactions.marineunit

import com.informatique.mtcit.business.transactions.shared.MarineUnit

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
            override val reason = "الوحدة البحرية غير مسجلة باسمك"
            override val suggestion = "يرجى اختيار وحدة بحرية مملوكة لك"
        }

        data class AlreadyMortgaged(
            override val unit: MarineUnit,
            val bankName: String,
            val mortgageEndDate: String
        ) : Ineligible() {
            override val reason = "الوحدة البحرية مرهونة بالفعل لدى $bankName"
            override val suggestion = "يمكنك تقديم طلب فك الرهن أولاً"
        }

        data class NotMortgaged(
            override val unit: MarineUnit
        ) : Ineligible() {
            override val reason = "الوحدة البحرية غير مرهونة"
            override val suggestion = "هذه المعاملة لفك الرهن فقط. اختر وحدة مرهونة"
        }

        data class TemporaryRegistration(
            override val unit: MarineUnit
        ) : Ineligible() {
            override val reason = "الوحدة لديها شهادة تسجيل مؤقتة فقط"
            override val suggestion = "يجب الحصول على شهادة تسجيل دائمة أولاً"
        }

        data class SuspendedOrCancelled(
            override val unit: MarineUnit,
            val status: String
        ) : Ineligible() {
            override val reason = "حالة التسجيل: $status"
            override val suggestion = "لا يمكن إجراء هذه المعاملة على وحدة متوقفة أو ملغاة"
        }

        data class HasViolations(
            override val unit: MarineUnit,
            val violationsCount: Int
        ) : Ineligible() {
            override val reason = "لا يمكن رهن وحدة بحرية لديها مخالفات نشطة ($violationsCount مخالفة)"
            override val suggestion = "يجب تسوية المخالفات أولاً قبل تقديم طلب الرهن"
        }

        data class HasDetentions(
            override val unit: MarineUnit,
            val detentionsCount: Int
        ) : Ineligible() {
            override val reason = "لا يمكن رهن وحدة بحرية محتجزة ($detentionsCount احتجاز)"
            override val suggestion = "يجب فك الاحتجاز أولاً قبل تقديم طلب الرهن"
        }

        data class CustomError(
            override val unit: MarineUnit,
            override val reason: String,
            override val suggestion: String? = null
        ) : Ineligible()
    }
}
