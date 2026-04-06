package com.informatique.mtcit.business.transactions.marineunit.rules

import com.informatique.mtcit.business.transactions.marineunit.*
import com.informatique.mtcit.business.transactions.shared.MarineUnit
import com.informatique.mtcit.business.transactions.TransactionType
import com.informatique.mtcit.business.transactions.marineunit.MarineUnitNavigationAction.RouteToConditionalStep
import com.informatique.mtcit.data.repository.MarineUnitRepository
import com.informatique.mtcit.data.repository.MortgageRepository
import javax.inject.Inject
import com.informatique.mtcit.common.util.AppLanguage
import com.informatique.mtcit.common.util.AppLanguage.isArabic

/**
 * Business rules for Mortgage Certificate transaction
 * Validates that marine unit is eligible to be mortgaged
 *
 * Backend APIs called:
 * - /api/marine-units/{unitId}/verify-ownership
 * - /api/marine-units/{unitId}/status
 * - /api/marine-units/{unitId}/registration-type
 * - /api/mortgage/check-status/{unitId}
 */
class MortgageCertificateRules @Inject constructor(
    marineUnitRepository: MarineUnitRepository,
    private val mortgageRepository: MortgageRepository
) : BaseMarineUnitRules(marineUnitRepository) {

    override suspend fun validateUnit(
        unit: MarineUnit,
        userId: String
    ): MarineUnitValidationResult {

        // Check 1: Ownership verification (Backend API call)
        checkOwnership(unit, userId)?.let { return it }

        // Check 2: Registration status must be ACTIVE (Backend API call)
        checkRegistrationStatus(unit)?.let { return it }

        // Check 3: Must have permanent registration (Backend API call)
        checkPermanentRegistration(unit)?.let { return it }

        // Check 4: Must NOT be mortgaged already (Backend API call)
        // API: /api/mortgage/check-status/{unitId}
        val mortgageStatus = mortgageRepository.getMortgageStatus(unit.id.toString())
        if (mortgageStatus.isMortgaged) {
            return MarineUnitValidationResult.Ineligible.AlreadyMortgaged(
                unit = unit,
                bankName = mortgageStatus.bankName ?: if (isArabic) "غير معروف" else "Unknown",
                mortgageEndDate = mortgageStatus.endDate ?: if (isArabic) "غير محدد" else "Not Specified"
            )
        }

        // Check 5: Must NOT have active violations
        val violationsCount = unit.violationsCount?.toIntOrNull() ?: 0
        if (violationsCount > 0) {
            return MarineUnitValidationResult.Ineligible.HasViolations(
                unit = unit,
                violationsCount = violationsCount
            )
        }

        // Check 6: Must NOT have active detentions
        val detentionsCount = unit.detentionsCount?.toIntOrNull() ?: 0
        if (detentionsCount > 0) {
            return MarineUnitValidationResult.Ineligible.HasDetentions(
                unit = unit,
                detentionsCount = detentionsCount
            )
        }

        // All checks passed - Unit is eligible for mortgage
        return MarineUnitValidationResult.Eligible(
            unit = unit,
            additionalData = mapOf(
                "registrationType" to "PERMANENT",
                "mortgageStatus" to "FREE",
                "canProceed" to true
            )
        )
    }

    override fun getNavigationAction(
        result: MarineUnitValidationResult
    ): MarineUnitNavigationAction {
        return when (result) {
            is MarineUnitValidationResult.Eligible -> {
                // Proceed to mortgage data step (bank details, amount, etc.)
                MarineUnitNavigationAction.ProceedToNextStep(
                    selectedUnit = result.unit,
                    additionalData = result.additionalData
                )
            }

            is MarineUnitValidationResult.Ineligible.AlreadyMortgaged -> {
                // Show RequestDetailScreen instead of dialog
                MarineUnitNavigationAction.ShowComplianceDetailScreen(
                    marineUnit = result.unit,
                    complianceIssues = listOf(
                        com.informatique.mtcit.business.transactions.marineunit.ComplianceIssue(
                            category = if (isArabic) "حالة الرهن" else "Mortgage Status",
                            title = if (isArabic) "الوحدة مرهونة بالفعل" else "Unit Already Mortgaged",
                            description = if (AppLanguage.isArabic) "هذه الوحدة البحرية مرهونة حالياً لدى ${result.bankName}" else "This marine unit is currently mortgaged to ${result.bankName}",
                            severity = com.informatique.mtcit.business.transactions.marineunit.IssueSeverity.BLOCKING,
                            details = mapOf(
                                (if (isArabic) "البنك المرتهن" else "Mortgagee Bank") to result.bankName,
                                (if (isArabic) "تاريخ انتهاء الرهن" else "Mortgage Expiry Date") to result.mortgageEndDate,
                                (if (isArabic) "الحل المقترح" else "Suggested Solution") to if (isArabic) "يمكنك تقديم طلب فك الرهن أولاً" else "You can submit a mortgage release request first"
                            )
                        )
                    ),
                    rejectionReason = if (AppLanguage.isArabic) "لا يمكن رهن وحدة بحرية مرهونة بالفعل. يجب فك الرهن الحالي أولاً." else "Cannot mortgage an already mortgaged unit. Current mortgage must be released first.",
                    rejectionTitle = if (AppLanguage.isArabic) "تم رفض الطلب - الوحدة مرهونة" else "Request Rejected - Unit Mortgaged"
                )
            }

            is MarineUnitValidationResult.Ineligible.NotOwned -> {
                // Show RequestDetailScreen for ownership issue
                MarineUnitNavigationAction.ShowComplianceDetailScreen(
                    marineUnit = result.unit,
                    complianceIssues = listOf(
                        com.informatique.mtcit.business.transactions.marineunit.ComplianceIssue(
                            category = if (isArabic) "الملكية" else "Ownership",
                            title = if (isArabic) "الوحدة غير مملوكة لك" else "Unit Not Owned by You",
                            description = if (AppLanguage.isArabic) "هذه الوحدة البحرية غير مسجلة باسمك في السجلات الرسمية" else "This marine unit is not registered in your name in official records",
                            severity = com.informatique.mtcit.business.transactions.marineunit.IssueSeverity.BLOCKING,
                            details = mapOf(
                                (if (isArabic) "السبب" else "Reason") to if (isArabic) "الوحدة البحرية غير مسجلة باسمك" else "Marine unit is not registered in your name",
                                "المالك الحالي" to (result.actualOwner ?: if (isArabic) "غير معروف" else "Unknown"),
                                (if (isArabic) "الحل المقترح" else "Suggested Solution") to if (isArabic) "يرجى اختيار وحدة بحرية مملوكة لك" else "Please select a marine unit owned by you"
                            )
                        )
                    ),
                    rejectionReason = if (AppLanguage.isArabic) "يمكنك رهن الوحدات البحرية المملوكة لك فقط. يرجى اختيار وحدة أخرى." else "You can only mortgage marine units owned by you. Please select another unit.",
                    rejectionTitle = if (AppLanguage.isArabic) "تم رفض الطلب - ملكية غير مثبتة" else "Request Rejected - Ownership Not Verified"
                )
            }

            is MarineUnitValidationResult.Ineligible.TemporaryRegistration -> {
                // Show RequestDetailScreen for temporary registration
                MarineUnitNavigationAction.ShowComplianceDetailScreen(
                    marineUnit = result.unit,
                    complianceIssues = listOf(
                        com.informatique.mtcit.business.transactions.marineunit.ComplianceIssue(
                            category = if (isArabic) "نوع التسجيل" else "Registration Type",
                            title = if (isArabic) "تسجيل مؤقت فقط" else "Temporary Registration Only",
                            description = if (AppLanguage.isArabic) "هذه الوحدة البحرية لديها شهادة تسجيل مؤقتة" else "This marine unit has a temporary registration certificate",
                            severity = com.informatique.mtcit.business.transactions.marineunit.IssueSeverity.BLOCKING,
                            details = mapOf(
                                (if (isArabic) "نوع التسجيل الحالي" else "Current Registration Type") to if (isArabic) "مؤقت" else "Temporary",
                                (if (isArabic) "المطلوب" else "Required") to if (isArabic) "تسجيل دائم" else "Permanent Registration",
                                (if (isArabic) "الحل المقترح" else "Suggested Solution") to if (isArabic) "يجب الحصول على شهادة تسجيل دائمة أولاً" else "Must obtain a permanent registration certificate first"
                            )
                        )
                    ),
                    rejectionReason = if (AppLanguage.isArabic) "لا يمكن رهن وحدة بحرية ذات تسجيل مؤقت. يجب أن يكون التسجيل دائماً." else "Cannot mortgage a unit with temporary registration. Registration must be permanent.",
                    rejectionTitle = if (AppLanguage.isArabic) "تم رفض الطلب - تسجيل مؤقت" else "Request Rejected - Temporary Registration"
                )
            }

            is MarineUnitValidationResult.Ineligible.SuspendedOrCancelled -> {
                // Show RequestDetailScreen for suspended/cancelled units
                MarineUnitNavigationAction.ShowComplianceDetailScreen(
                    marineUnit = result.unit,
                    complianceIssues = listOf(
                        com.informatique.mtcit.business.transactions.marineunit.ComplianceIssue(
                            category = if (isArabic) "حالة التسجيل" else "Registration Status",
                            title = if (AppLanguage.isArabic) "التسجيل ${result.status}" else "Registration ${result.status}",
                            description = if (AppLanguage.isArabic) "هذه الوحدة البحرية في حالة ${result.status}" else "This marine unit is in ${result.status} status",
                            severity = com.informatique.mtcit.business.transactions.marineunit.IssueSeverity.BLOCKING,
                            details = mapOf(
                                (if (isArabic) "الحالة" else "Status") to result.status,
                                (if (isArabic) "الحل المقترح" else "Suggested Solution") to if (AppLanguage.isArabic) "لا يمكن إجراء هذه المعاملة على وحدة ${result.status}" else "Cannot perform this transaction on a ${result.status} unit"
                            )
                        )
                    ),
                    rejectionReason = if (AppLanguage.isArabic) "لا يمكن رهن وحدة بحرية ${result.status}. يجب أن تكون الوحدة نشطة." else "Cannot mortgage a ${result.status} marine unit. Unit must be active.",
                    rejectionTitle = if (AppLanguage.isArabic) "تم رفض الطلب - وحدة ${result.status}" else "Request Rejected - Unit ${result.status}"
                )
            }

            is MarineUnitValidationResult.Ineligible.HasViolations -> {
                // Show RequestDetailScreen for units with violations
                MarineUnitNavigationAction.ShowComplianceDetailScreen(
                    marineUnit = result.unit,
                    complianceIssues = listOf(
                        com.informatique.mtcit.business.transactions.marineunit.ComplianceIssue(
                            category = if (isArabic) "المخالفات" else "Violations",
                            title = if (isArabic) "وجود مخالفات نشطة" else "Active Violations Exist",
                            description = if (AppLanguage.isArabic) "هذه الوحدة البحرية لديها ${result.violationsCount} مخالفة نشطة" else "This marine unit has ${result.violationsCount} active violations",
                            severity = com.informatique.mtcit.business.transactions.marineunit.IssueSeverity.BLOCKING,
                            details = mapOf(
                                (if (isArabic) "عدد المخالفات" else "Number of Violations") to result.violationsCount.toString(),
                                (if (isArabic) "الحل المقترح" else "Suggested Solution") to if (AppLanguage.isArabic) "يجب تسوية جميع المخالفات قبل تقديم طلب الرهن" else "All violations must be settled before submitting a mortgage request"
                            )
                        )
                    ),
                    rejectionReason = if (AppLanguage.isArabic) "لا يمكن رهن وحدة بحرية لديها مخالفات نشطة. يجب تسوية المخالفات أولاً." else "Cannot mortgage a unit with active violations. Violations must be settled first.",
                    rejectionTitle = if (AppLanguage.isArabic) "تم رفض الطلب - مخالفات نشطة" else "Request Rejected - Active Violations"
                )
            }

            is MarineUnitValidationResult.Ineligible.HasDetentions -> {
                // Show RequestDetailScreen for units with detentions
                MarineUnitNavigationAction.ShowComplianceDetailScreen(
                    marineUnit = result.unit,
                    complianceIssues = listOf(
                        com.informatique.mtcit.business.transactions.marineunit.ComplianceIssue(
                            category = if (isArabic) "الاحتجازات" else "Detentions",
                            title = if (isArabic) "وجود احتجازات نشطة" else "Active Detentions Exist",
                            description = if (AppLanguage.isArabic) "هذه الوحدة البحرية محتجزة (${result.detentionsCount} احتجاز)" else "This marine unit is detained (${result.detentionsCount} detentions)",
                            severity = com.informatique.mtcit.business.transactions.marineunit.IssueSeverity.BLOCKING,
                            details = mapOf(
                                (if (AppLanguage.isArabic) "عدد الاحتجازات" else "Number of Detentions") to result.detentionsCount.toString(),
                                (if (isArabic) "الحل المقترح" else "Suggested Solution") to if (AppLanguage.isArabic) "يجب فك جميع الاحتجازات قبل تقديم طلب الرهن" else "All detentions must be released before submitting a mortgage request"
                            )
                        )
                    ),
                    rejectionReason = if (AppLanguage.isArabic) "لا يمكن رهن وحدة بحرية محتجزة. يجب فك الاحتجاز أولاً." else "Cannot mortgage a detained unit. Detention must be released first.",
                    rejectionTitle = if (AppLanguage.isArabic) "تم رفض الطلب - وحدة محتجزة" else "Request Rejected - Detained Unit"
                )
            }

            is MarineUnitValidationResult.Ineligible -> {
                // Generic ineligible case - show RequestDetailScreen
                MarineUnitNavigationAction.ShowComplianceDetailScreen(
                    marineUnit = result.unit,
                    complianceIssues = listOf(
                        com.informatique.mtcit.business.transactions.marineunit.ComplianceIssue(
                            category = if (isArabic) "سبب الرفض" else "Rejection Reason",
                            title = if (isArabic) "الوحدة غير مؤهلة" else "Unit Not Eligible",
                            description = result.reason,
                            severity = com.informatique.mtcit.business.transactions.marineunit.IssueSeverity.BLOCKING,
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

    override fun getStepTitle(): String = if (isArabic) "اختيار الوحدة البحرية للرهن" else "Select Marine Unit for Mortgage"

    override fun getStepDescription(): String = if (isArabic) "اختر الوحدة البحرية التي ترغب في رهنها" else "Select the marine unit you want to mortgage"
}
