package com.informatique.mtcit.business.transactions.marineunit.rules

import com.informatique.mtcit.business.transactions.marineunit.*
import com.informatique.mtcit.business.transactions.shared.MarineUnit
import com.informatique.mtcit.data.repository.MarineUnitRepository
import javax.inject.Inject

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
                reason = "الوحدة البحرية غير مفحوصة",
                suggestion = "يجب إجراء الفحص أولاً قبل التسجيل المؤقت"
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
                reason = "الطلب قيد المعالجة",
                suggestion = "يرجى الانتظار حتى اكتمال عملية التحقق من الفحص"
            )
        }

        // Scenario 3: Not verified or rejected (any other status)
        return MarineUnitValidationResult.Ineligible.CustomError(
            unit = unit,
            reason = "الوحدة البحرية غير مفحوصة أو تم رفض الفحص",
            suggestion = inspectionStatus.remarks ?: "يجب إجراء الفحص أولاً قبل التسجيل المؤقت"
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
                val isPending = result.reason.contains("قيد المعالجة")

                MarineUnitNavigationAction.ShowComplianceDetailScreen(
                    marineUnit = result.unit,
                    complianceIssues = listOf(
                        ComplianceIssue(
                            category = "حالة الفحص",
                            title = if (isPending) "الطلب قيد المعالجة" else "الوحدة غير مفحوصة",
                            description = result.reason,
                            severity = if (isPending) IssueSeverity.WARNING else IssueSeverity.BLOCKING,
                            details = result.suggestion?.let {
                                mapOf("الحل المقترح" to it)
                            } ?: emptyMap()
                        )
                    ),
                    rejectionReason = result.reason + (result.suggestion?.let { "\n\n$it" } ?: ""),
                    rejectionTitle = if (isPending) "طلب قيد المعالجة" else "تم رفض الطلب"
                )
            }

            // Handle other ineligible cases (though we don't use them now)
            is MarineUnitValidationResult.Ineligible -> {
                MarineUnitNavigationAction.ShowComplianceDetailScreen(
                    marineUnit = result.unit,
                    complianceIssues = listOf(
                        ComplianceIssue(
                            category = "سبب الرفض",
                            title = "الوحدة غير مؤهلة",
                            description = result.reason,
                            severity = IssueSeverity.BLOCKING,
                            details = result.suggestion?.let {
                                mapOf("الحل المقترح" to it)
                            } ?: emptyMap()
                        )
                    ),
                    rejectionReason = result.reason + (result.suggestion?.let { "\n\n$it" } ?: ""),
                    rejectionTitle = "تم رفض الطلب"
                )
            }
        }
    }

    override fun allowMultipleSelection(): Boolean = false

    override fun getStepTitle(): String = "اختيار الوحدة البحرية للتسجيل المؤقت"

    override fun getStepDescription(): String = "اختر الوحدة البحرية التي ترغب في تسجيلها مؤقتاً"
}
