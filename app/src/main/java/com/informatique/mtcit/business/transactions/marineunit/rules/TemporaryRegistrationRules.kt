package com.informatique.mtcit.business.transactions.marineunit.rules

import com.informatique.mtcit.business.transactions.marineunit.*
import com.informatique.mtcit.business.transactions.shared.MarineUnit
import com.informatique.mtcit.data.repository.MarineUnitRepository
import javax.inject.Inject
import com.informatique.mtcit.common.util.AppLanguage
import com.informatique.mtcit.common.util.AppLanguage.isArabic

/**
 * Business rules for Temporary Registration Certificate transaction
 * Validates marine unit based on inspection status ONLY
 *
 * Backend API called:
 * - /api/marine-units/{unitId}/inspection-status
 *
 * Three scenarios:
 * 1. isInspected = true (verified) → Proceed to next step ✓
 * 2. isInspected = false AND status = "PENDING" → Show "Under Verification"
 * 3. isInspected = false AND status != "PENDING" → Show "Request Declined"
 */
class TemporaryRegistrationRules @Inject constructor(
    marineUnitRepository: MarineUnitRepository
) : BaseMarineUnitRules(marineUnitRepository) {

    override suspend fun validateUnit(
        unit: MarineUnit,
        userId: String
    ): MarineUnitValidationResult {

        // ✅ Special case: NEW marine unit (being added for the first time)
        // New units have temporary IDs like "new_1234567890"
        // They don't exist in database yet, so they're automatically "not verified"
        if (unit.id.startsWith("new_")) {
            println("🆕 NEW marine unit detected: ${unit.name}, treating as not verified")
            return MarineUnitValidationResult.Ineligible.CustomError(
                unit = unit,
                reason = if (isArabic) "الوحدة البحرية غير مفحوصة" else "Marine unit has not been inspected",
                suggestion = if (isArabic) "يجب إجراء الفحص أولاً قبل التسجيل المؤقت" else "Inspection must be performed first"
            )
        }

        // ONLY Check: Get inspection status (Backend API call)
        // API: /api/marine-units/{unitId}/inspection-status
        val inspectionStatus = checkInspectionStatus(unit)

        // Scenario 1: Unit is verified (inspected and valid)
        if (inspectionStatus.isInspected && inspectionStatus.status == "VALID") {
            return MarineUnitValidationResult.Eligible(
                unit = unit,
                additionalData = mapOf(
                    "isInspected" to true,
                    "inspectionDate" to (inspectionStatus.inspectionDate ?: ""),
                    "inspectionType" to (inspectionStatus.inspectionType ?: ""),
                    "certificateNumber" to (inspectionStatus.certificateNumber ?: ""),
                    "inspectionStatus" to "VALID",
                    "canProceed" to true
                )
            )
        }

        // Scenario 2: Under verification (PENDING)
        if (!inspectionStatus.isInspected && inspectionStatus.status == "PENDING") {
            return MarineUnitValidationResult.Ineligible.CustomError(
                unit = unit,
                reason = if (AppLanguage.isArabic) "الطلب قيد المعالجة" else "Request Under Processing",
                suggestion = if (isArabic) "يرجى الانتظار حتى اكتمال عملية التحقق من الفحص" else "Please wait until the inspection verification is complete"
            )
        }

        // Scenario 3: Not verified or rejected (any other status)
        return MarineUnitValidationResult.Ineligible.CustomError(
            unit = unit,
            reason = if (isArabic) "الوحدة البحرية غير مفحوصة أو تم رفض الفحص" else "Marine unit has not been inspected or inspection was rejected",
            suggestion = inspectionStatus.remarks ?: if (isArabic) "يجب إجراء الفحص أولاً قبل التسجيل المؤقت" else "Inspection must be performed first"
        )
    }

    override fun getNavigationAction(
        result: MarineUnitValidationResult
    ): MarineUnitNavigationAction {
        return when (result) {
            is MarineUnitValidationResult.Eligible -> {
                // Scenario 1: Unit is verified - proceed to next step
                MarineUnitNavigationAction.ProceedToNextStep(
                    selectedUnit = result.unit,
                    additionalData = result.additionalData
                )
            }

            is MarineUnitValidationResult.Ineligible.CustomError -> {
                // Scenario 2 & 3: Show RequestDetailScreen with appropriate message
                val isPending = result.reason.contains(if (AppLanguage.isArabic) "قيد المعالجة" else "Under Processing")

                MarineUnitNavigationAction.ShowComplianceDetailScreen(
                    marineUnit = result.unit,
                    complianceIssues = listOf(
                        ComplianceIssue(
                            category = if (isArabic) "حالة الفحص" else "Inspection Status",
                            title = if (isPending) if (AppLanguage.isArabic) "الطلب قيد المعالجة" else "Request Under Processing" else "الوحدة غير مفحوصة",
                            description = result.reason,
                            severity = if (isPending) IssueSeverity.WARNING else IssueSeverity.BLOCKING,
                            details = result.suggestion?.let {
                                mapOf((if (isArabic) "الحل المقترح" else "Suggested Solution") to it)
                            } ?: emptyMap()
                        )
                    ),
                    rejectionReason = result.reason + (result.suggestion?.let { "\n\n$it" } ?: ""),
                    rejectionTitle = if (isPending) if (AppLanguage.isArabic) "طلب قيد المعالجة" else "Request Under Processing" else if (AppLanguage.isArabic) "تم رفض الطلب" else "Request Rejected"
                )
            }

            // Handle other ineligible cases (though we don't use them now)
            is MarineUnitValidationResult.Ineligible -> {
                MarineUnitNavigationAction.ShowComplianceDetailScreen(
                    marineUnit = result.unit,
                    complianceIssues = listOf(
                        ComplianceIssue(
                            category = if (isArabic) "سبب الرفض" else "Rejection Reason",
                            title = if (isArabic) "الوحدة غير مؤهلة" else "Unit Not Eligible",
                            description = result.reason,
                            severity = IssueSeverity.BLOCKING,
                            details = result.suggestion?.let {
                                mapOf((if (isArabic) "الحل المقترح" else "Suggested Solution") to it)
                            } ?: emptyMap()
                        )
                    ),
                    rejectionReason = result.reason + (result.suggestion?.let { "\n\n$it" } ?: ""),
                    rejectionTitle = if (AppLanguage.isArabic) "تم رفض الطلب" else "Request Rejected"
                )
            }
        }
    }

    override fun allowMultipleSelection(): Boolean = false

    override fun getStepTitle(): String = if (isArabic) "اختيار الوحدة البحرية للتسجيل المؤقت" else "Select Marine Unit for Temporary Registration"

    override fun getStepDescription(): String = if (isArabic) "اختر الوحدة البحرية التي ترغب في تسجيلها مؤقتاً" else "Select the marine unit you want to temporarily register"
}
