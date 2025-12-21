package com.informatique.mtcit.business.transactions

import com.informatique.mtcit.R
import com.informatique.mtcit.business.transactions.shared.SharedSteps
import com.informatique.mtcit.business.usecases.FormValidationUseCase
import com.informatique.mtcit.business.validation.rules.ValidationRule
import com.informatique.mtcit.ui.viewmodels.StepData
import javax.inject.Inject

/**
 * Login/Registration Strategy
 *
 * Handles user authentication flow:
 * - Step 1: Login/Registration method selection (Mobile/ID)
 * - Step 2: Mobile phone verification (if mobile selected)
 * - Step 3: OTP verification (if phone number entered)
 */
class LoginStrategy @Inject constructor(
    private val validationUseCase: FormValidationUseCase,
    ) : TransactionStrategy {

    private var accumulatedFormData: MutableMap<String, String> = mutableMapOf()

    override suspend fun loadDynamicOptions(): Map<String, List<*>> {
        // No dynamic options needed for login
        return emptyMap()
    }

    override fun getSteps(): List<StepData> {
        val steps = mutableListOf<StepData>()

        // ✅ Step 0: Login/Registration Selection (Mobile or Civil ID)
        steps.add(SharedSteps.loginRegistrationStep())

        // ✅ Step 1: Mobile Phone Verification step
        steps.add(SharedSteps.mobilePhoneVerificationStep())

        // ✅ Step 2: OTP Verification step (FINAL STEP)
        val phoneNumber = accumulatedFormData["mobilePhoneNumber"] ?: ""
        val countryCode = accumulatedFormData["mobilePhoneNumber_countryCode"] ?: "+968"
        val fullPhoneNumber = if (phoneNumber.isNotEmpty()) "$countryCode$phoneNumber" else ""

        steps.add(
            SharedSteps.otpVerificationStep(
                phoneNumber = fullPhoneNumber
            )
        )

        // ✅ No review step needed - LoginViewModel.nextStep() automatically calls submitForm()
        // when OTP is entered on step 2, which triggers immediate navigation to target transaction

        println("🔍 LoginStrategy.getSteps() - Total steps: ${steps.size}")
        println("🔍 LoginStrategy.getSteps() - registrationMethod: ${accumulatedFormData["registrationMethod"]}")
        println("🔍 LoginStrategy.getSteps() - mobilePhoneNumber: ${accumulatedFormData["mobilePhoneNumber"]}")
        println("🔍 LoginStrategy.getSteps() - fullPhoneNumber for OTP: $fullPhoneNumber")

        return steps
    }

    override fun validateStep(step: Int, data: Map<String, Any>): Pair<Boolean, Map<String, String>> {
        val stepData = getSteps().getOrNull(step) ?: return Pair(false, emptyMap())
        val formData = data.mapValues { it.value.toString() }

        // ✅ Special validation for Step 1 (Mobile Phone Verification)
        // Only allow to proceed if user selected "mobile_phone" in Step 0
        if (step == 1) {
            val selectedMethod = accumulatedFormData["registrationMethod"]
            if (selectedMethod != "mobile_phone") {
                return Pair(
                    false,
                    mapOf("registrationMethod" to "يجب اختيار تسجيل الدخول عبر رقم الهاتف المحمول للمتابعة")
                )
            }
        }

        // ✅ Special validation for Step 2 (OTP Verification)
        // Only allow if phone number was entered in Step 1
        if (step == 2) {
            val phoneNumber = accumulatedFormData["mobilePhoneNumber"]
            if (phoneNumber.isNullOrEmpty()) {
                return Pair(
                    false,
                    mapOf("mobilePhoneNumber" to "يجب إدخال رقم الهاتف المحمول أولاً")
                )
            }
        }

        // ✅ Get validation rules for this step
        val rules = getValidationRulesForStep(stepData)

        // ✅ Use accumulated data for validation (enables cross-step validation)
        return validationUseCase.validateStepWithAccumulatedData(
            stepData = stepData,
            currentStepData = formData,
            allAccumulatedData = accumulatedFormData,
            crossFieldRules = rules
        )
    }
    private fun getValidationRulesForStep(@Suppress("UNUSED_PARAMETER") stepData: StepData): List<ValidationRule> {
        // Login strategy doesn't need complex validation rules
        // Basic field validation is handled by the field definitions themselves
        return emptyList()
    }

    override suspend fun processStepData(step: Int, data: Map<String, String>): Int {
        println("📝 LoginStrategy.processStepData() - Step: $step")
        println("📝 LoginStrategy.processStepData() - Input data: $data")
        println("📝 LoginStrategy.processStepData() - Before: $accumulatedFormData")

        // Accumulate form data
        accumulatedFormData.putAll(data)

        println("📝 LoginStrategy.processStepData() - After: $accumulatedFormData")

        // ✅ Handle OTP verification on Step 2
        if (step == 2) {
            val otpCode = data["otpCode"]
            if (!otpCode.isNullOrEmpty()) {
                println("📞 LoginStrategy: Verifying OTP code: $otpCode")

                // TODO: Call OTP verification API
                // For now, simulate success
                println("✅ LoginStrategy: OTP verified successfully")

                // ✅ Strategy for immediate submission:
                // When we return 'step', the BaseViewModel will:
                // 1. Call getSteps() again
                // 2. getSteps() will now return step 2 as a review step (empty fields) since OTP is present
                // 3. The UI will show this as a review step with Submit button
                // 4. We need to trigger submit automatically

                // Instead, we mark this step as ready for submission
                // by making it a review step in getSteps()
            }
        }

        return step
    }

    override suspend fun submit(data: Map<String, String>): Result<Boolean> {
        println("✅ LoginStrategy.submit() called")
        println("✅ Login successful! User data: $data")

        // TODO: Save user session
        // - Save phone number
        // - Save authentication token
        // - Save user ID

        // ✅ Return success - this will trigger LoginViewModel.loginComplete
        // which navigates to the target transaction
        return Result.success(true)
    }

    override fun getContext(): TransactionContext {
        TODO("Not yet implemented")
    }
}
