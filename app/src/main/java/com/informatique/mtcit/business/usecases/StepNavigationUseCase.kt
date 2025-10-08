package com.informatique.mtcit.business.usecases

import com.informatique.mtcit.ui.viewmodels.StepData
import javax.inject.Inject

/**
 * Use case for handling step navigation logic
 */
class StepNavigationUseCase @Inject constructor(
    private val validationUseCase: FormValidationUseCase
) {

    /**
     * Determines if user can proceed to next step
     * Only checks if mandatory fields are filled, not if they're valid
     */
    fun canProceedToNext(
        currentStep: Int,
        steps: List<StepData>,
        formData: Map<String, String>
    ): Boolean {
        val stepData = steps.getOrNull(currentStep) ?: return false
        // Just check if mandatory fields are filled, not validated
        return validationUseCase.areMandatoryFieldsFilled(stepData, formData)
    }

    /**
     * Calculates the next step index
     */
    fun getNextStep(currentStep: Int, totalSteps: Int): Int? {
        val nextStep = currentStep + 1
        return if (nextStep < totalSteps) nextStep else null
    }

    /**
     * Calculates the previous step index
     */
    fun getPreviousStep(currentStep: Int): Int? {
        return if (currentStep > 0) currentStep - 1 else null
    }

    /**
     * Validates if user can jump to a specific step
     */
    fun canJumpToStep(
        targetStep: Int,
        currentStep: Int,
        completedSteps: Set<Int>,
        totalSteps: Int
    ): Boolean {
        return targetStep in 0 until totalSteps &&
                (targetStep <= currentStep || targetStep in completedSteps)
    }
}
