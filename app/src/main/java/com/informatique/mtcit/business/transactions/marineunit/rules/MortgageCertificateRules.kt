package com.informatique.mtcit.business.transactions.marineunit.rules

import com.informatique.mtcit.business.transactions.marineunit.*
import com.informatique.mtcit.business.transactions.shared.MarineUnit
import com.informatique.mtcit.business.transactions.TransactionType
import com.informatique.mtcit.business.transactions.marineunit.MarineUnitNavigationAction.RouteToConditionalStep
import com.informatique.mtcit.data.repository.MarineUnitRepository
import com.informatique.mtcit.data.repository.MortgageRepository
import javax.inject.Inject

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
        val mortgageStatus = mortgageRepository.getMortgageStatus(unit.id)
        if (mortgageStatus.isMortgaged) {
            return MarineUnitValidationResult.Ineligible.AlreadyMortgaged(
                unit = unit,
                bankName = mortgageStatus.bankName ?: "غير معروف",
                mortgageEndDate = mortgageStatus.endDate ?: "غير محدد"
            )
        }

        // Check 5: Must NOT have active violations
        val violationsCount = unit.violationsCount.toIntOrNull() ?: 0
        if (violationsCount > 0) {
            return MarineUnitValidationResult.Ineligible.HasViolations(
                unit = unit,
                violationsCount = violationsCount
            )
        }

        // Check 6: Must NOT have active detentions
        val detentionsCount = unit.detentionsCount.toIntOrNull() ?: 0
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
                            category = "حالة الرهن",
                            title = "الوحدة مرهونة بالفعل",
                            description = "هذه الوحدة البحرية مرهونة حالياً لدى ${result.bankName}",
                            severity = com.informatique.mtcit.business.transactions.marineunit.IssueSeverity.BLOCKING,
                            details = mapOf(
                                "البنك المرتهن" to result.bankName,
                                "تاريخ انتهاء الرهن" to result.mortgageEndDate,
                                "الحل المقترح" to "يمكنك تقديم طلب فك الرهن أولاً"
                            )
                        )
                    ),
                    rejectionReason = "لا يمكن رهن وحدة بحرية مرهونة بالفعل. يجب فك الرهن الحالي أولاً.",
                    rejectionTitle = "تم رفض الطلب - الوحدة مرهونة"
                )
            }

            is MarineUnitValidationResult.Ineligible.NotOwned -> {
                // Show RequestDetailScreen for ownership issue
                MarineUnitNavigationAction.ShowComplianceDetailScreen(
                    marineUnit = result.unit,
                    complianceIssues = listOf(
                        com.informatique.mtcit.business.transactions.marineunit.ComplianceIssue(
                            category = "الملكية",
                            title = "الوحدة غير مملوكة لك",
                            description = "هذه الوحدة البحرية غير مسجلة باسمك في السجلات الرسمية",
                            severity = com.informatique.mtcit.business.transactions.marineunit.IssueSeverity.BLOCKING,
                            details = mapOf(
                                "السبب" to "الوحدة البحرية غير مسجلة باسمك",
                                "المالك الحالي" to (result.actualOwner ?: "غير معروف"),
                                "الحل المقترح" to "يرجى اختيار وحدة بحرية مملوكة لك"
                            )
                        )
                    ),
                    rejectionReason = "يمكنك رهن الوحدات البحرية المملوكة لك فقط. يرجى اختيار وحدة أخرى.",
                    rejectionTitle = "تم رفض الطلب - ملكية غير مثبتة"
                )
            }

            is MarineUnitValidationResult.Ineligible.TemporaryRegistration -> {
                // Show RequestDetailScreen for temporary registration
                MarineUnitNavigationAction.ShowComplianceDetailScreen(
                    marineUnit = result.unit,
                    complianceIssues = listOf(
                        com.informatique.mtcit.business.transactions.marineunit.ComplianceIssue(
                            category = "نوع التسجيل",
                            title = "تسجيل مؤقت فقط",
                            description = "هذه الوحدة البحرية لديها شهادة تسجيل مؤقتة",
                            severity = com.informatique.mtcit.business.transactions.marineunit.IssueSeverity.BLOCKING,
                            details = mapOf(
                                "نوع التسجيل الحالي" to "مؤقت",
                                "المطلوب" to "تسجيل دائم",
                                "الحل المقترح" to "يجب الحصول على شهادة تسجيل دائمة أولاً"
                            )
                        )
                    ),
                    rejectionReason = "لا يمكن رهن وحدة بحرية ذات تسجيل مؤقت. يجب أن يكون التسجيل دائماً.",
                    rejectionTitle = "تم رفض الطلب - تسجيل مؤقت"
                )
            }

            is MarineUnitValidationResult.Ineligible.SuspendedOrCancelled -> {
                // Show RequestDetailScreen for suspended/cancelled units
                MarineUnitNavigationAction.ShowComplianceDetailScreen(
                    marineUnit = result.unit,
                    complianceIssues = listOf(
                        com.informatique.mtcit.business.transactions.marineunit.ComplianceIssue(
                            category = "حالة التسجيل",
                            title = "التسجيل ${result.status}",
                            description = "هذه الوحدة البحرية في حالة ${result.status}",
                            severity = com.informatique.mtcit.business.transactions.marineunit.IssueSeverity.BLOCKING,
                            details = mapOf(
                                "الحالة" to result.status,
                                "الحل المقترح" to "لا يمكن إجراء هذه المعاملة على وحدة ${result.status}"
                            )
                        )
                    ),
                    rejectionReason = "لا يمكن رهن وحدة بحرية ${result.status}. يجب أن تكون الوحدة نشطة.",
                    rejectionTitle = "تم رفض الطلب - وحدة ${result.status}"
                )
            }

            is MarineUnitValidationResult.Ineligible.HasViolations -> {
                // Show RequestDetailScreen for units with violations
                MarineUnitNavigationAction.ShowComplianceDetailScreen(
                    marineUnit = result.unit,
                    complianceIssues = listOf(
                        com.informatique.mtcit.business.transactions.marineunit.ComplianceIssue(
                            category = "المخالفات",
                            title = "وجود مخالفات نشطة",
                            description = "هذه الوحدة البحرية لديها ${result.violationsCount} مخالفة نشطة",
                            severity = com.informatique.mtcit.business.transactions.marineunit.IssueSeverity.BLOCKING,
                            details = mapOf(
                                "عدد المخالفات" to result.violationsCount.toString(),
                                "الحل المقترح" to "يجب تسوية جميع المخالفات قبل تقديم طلب الرهن"
                            )
                        )
                    ),
                    rejectionReason = "لا يمكن رهن وحدة بحرية لديها مخالفات نشطة. يجب تسوية المخالفات أولاً.",
                    rejectionTitle = "تم رفض الطلب - مخالفات نشطة"
                )
            }

            is MarineUnitValidationResult.Ineligible.HasDetentions -> {
                // Show RequestDetailScreen for units with detentions
                MarineUnitNavigationAction.ShowComplianceDetailScreen(
                    marineUnit = result.unit,
                    complianceIssues = listOf(
                        com.informatique.mtcit.business.transactions.marineunit.ComplianceIssue(
                            category = "الاحتجازات",
                            title = "وجود احتجازات نشطة",
                            description = "هذه الوحدة البحرية محتجزة (${result.detentionsCount} احتجاز)",
                            severity = com.informatique.mtcit.business.transactions.marineunit.IssueSeverity.BLOCKING,
                            details = mapOf(
                                "عدد الاحتجازات" to result.detentionsCount.toString(),
                                "الحل المقترح" to "يجب فك جميع الاحتجازات قبل تقديم طلب الرهن"
                            )
                        )
                    ),
                    rejectionReason = "لا يمكن رهن وحدة بحرية محتجزة. يجب فك الاحتجاز أولاً.",
                    rejectionTitle = "تم رفض الطلب - وحدة محتجزة"
                )
            }

            is MarineUnitValidationResult.Ineligible -> {
                // Generic ineligible case - show RequestDetailScreen
                MarineUnitNavigationAction.ShowComplianceDetailScreen(
                    marineUnit = result.unit,
                    complianceIssues = listOf(
                        com.informatique.mtcit.business.transactions.marineunit.ComplianceIssue(
                            category = "سبب الرفض",
                            title = "الوحدة غير مؤهلة",
                            description = result.reason,
                            severity = com.informatique.mtcit.business.transactions.marineunit.IssueSeverity.BLOCKING,
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

    override fun getStepTitle(): String = "اختيار الوحدة البحرية للرهن"

    override fun getStepDescription(): String = "اختر الوحدة البحرية التي ترغب في رهنها"
}
