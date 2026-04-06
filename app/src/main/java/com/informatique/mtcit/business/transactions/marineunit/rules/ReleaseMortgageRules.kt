package com.informatique.mtcit.business.transactions.marineunit.rules

import com.informatique.mtcit.business.transactions.marineunit.*
import com.informatique.mtcit.business.transactions.shared.MarineUnit
import com.informatique.mtcit.business.transactions.TransactionType
import com.informatique.mtcit.data.repository.MarineUnitRepository
import com.informatique.mtcit.data.repository.MortgageRepository
import javax.inject.Inject
import com.informatique.mtcit.common.util.AppLanguage
import com.informatique.mtcit.common.util.AppLanguage.isArabic

/**
 * Business rules for Release Mortgage transaction
 * Validates that marine unit has an active mortgage that can be released
 *
 * Backend APIs called:
 * - /api/marine-units/{unitId}/verify-ownership
 * - /api/marine-units/{unitId}/status
 * - /api/mortgage/check-status/{unitId}
 * - /api/mortgage/verify-bank/{bankId}
 */
class ReleaseMortgageRules @Inject constructor(
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

        // Check 3: MUST be mortgaged (opposite of MortgageCertificate)
        // Backend API: /api/mortgage/check-status/{unitId}
        val mortgageStatus = mortgageRepository.getMortgageStatus(unit.id.toString())
        if (!mortgageStatus.isMortgaged) {
            return MarineUnitValidationResult.Ineligible.NotMortgaged(unit)
        }

        // Check 4: Verify mortgage is with approved bank
        // Backend API: /api/mortgage/verify-bank/{bankId}
        if (!mortgageStatus.isApprovedBank) {
            return MarineUnitValidationResult.Ineligible.CustomError(
                unit = unit,
                reason = if (isArabic) "الرهن الحالي غير مسجل لدى بنك معتمد" else "Current mortgage is not registered with an approved bank",
                suggestion = if (isArabic) "يرجى التواصل مع الجهات المختصة لتحديث بيانات الرهن" else "Please contact the competent authorities to update mortgage data"
            )
        }

        // All checks passed - Unit can have mortgage released
        return MarineUnitValidationResult.Eligible(
            unit = unit,
            additionalData = mapOf(
                "mortgageId" to (mortgageStatus.mortgageId ?: ""),
                "bankName" to (mortgageStatus.bankName ?: ""),
                "mortgageStartDate" to (mortgageStatus.startDate ?: ""),
                "mortgageEndDate" to (mortgageStatus.endDate ?: ""),
                "mortgageAmount" to (mortgageStatus.mortgageAmount ?: "")
            )
        )
    }

    override fun getNavigationAction(
        result: MarineUnitValidationResult
    ): MarineUnitNavigationAction {
        return when (result) {
            is MarineUnitValidationResult.Eligible -> {
                // Proceed to Review step directly (no additional data needed)
                MarineUnitNavigationAction.ProceedToNextStep(
                    selectedUnit = result.unit,
                    additionalData = result.additionalData
                )
            }

            is MarineUnitValidationResult.Ineligible.NotMortgaged -> {
                // Suggest Mortgage Certificate transaction instead
                MarineUnitNavigationAction.ShowError(
                    title = if (isArabic) "الوحدة البحرية غير مرهونة" else "Marine unit is not mortgaged",
                    message = if (AppLanguage.isArabic) "${result.reason}\n\nلا يمكن فك رهن وحدة غير مرهونة" else "${result.reason}\n\nCannot release mortgage on a non-mortgaged unit",
                    actions = listOf(
                        ErrorAction(
                            label = if (isArabic) "الانتقال إلى إصدار شهادة رهن" else "Go to Mortgage Certificate Issuance",
                            action = MarineUnitNavigationAction.RedirectToTransaction(
                                transactionType = TransactionType.MORTGAGE_CERTIFICATE,
                                reason = "Unit not mortgaged",
                                prefilledData = mapOf("selectedMarineUnitId" to result.unit.id.toString())
                            )
                        ),
                        ErrorAction(
                            label = if (isArabic) "اختيار وحدة أخرى" else "Select Another Unit",
                            action = MarineUnitNavigationAction.ShowError(
                                title = "",
                                message = "",
                                actions = emptyList()
                            )
                        )
                    )
                )
            }

            is MarineUnitValidationResult.Ineligible -> {
                MarineUnitNavigationAction.ShowError(
                    title = if (isArabic) "الوحدة البحرية غير مؤهلة" else "Marine Unit Not Eligible",
                    message = "${result.reason}\n\n${result.suggestion ?: ""}",
                    actions = emptyList()
                )
            }
        }
    }

    override fun allowMultipleSelection(): Boolean = false

    override fun getStepTitle(): String = if (isArabic) "اختيار الوحدة البحرية لفك الرهن" else "Select Marine Unit for Mortgage Release"

    override fun getStepDescription(): String = if (isArabic) "اختر الوحدة البحرية المرهونة التي ترغب في فك رهنها" else "Select the mortgaged marine unit you want to release"
}

