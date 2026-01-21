package com.informatique.mtcit.business.transactions

import com.informatique.mtcit.business.transactions.shared.SharedSteps
import com.informatique.mtcit.business.transactions.shared.StepType
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
    private var oauthTriggered: Boolean = false // ✅ Flag to prevent re-triggering OAuth

    override suspend fun loadDynamicOptions(): Map<String, List<*>> {
        // No dynamic options needed for login
        return emptyMap()
    }

    override fun getSteps(): List<StepData> {
        val steps = mutableListOf<StepData>()

        // ✅ Step 0: Login/Registration Selection (Mobile or Civil ID)
        steps.add(SharedSteps.loginRegistrationStep())

        // ✅ Check which method was selected
        val selectedMethod = accumulatedFormData["registrationMethod"]

        println("🔍 LoginStrategy.getSteps() - registrationMethod: $selectedMethod")

        // ✅ If civil_id is selected, return only Step 0 (OAuth will be triggered directly)
        if (selectedMethod == "civil_id") {
            println("🔍 LoginStrategy.getSteps() - civil_id selected, returning 1 step only")
            return steps
        }

        // ✅ If mobile_phone is selected, include all steps (Mobile + OTP)
        if (selectedMethod == "mobile_phone") {
            println("🔍 LoginStrategy.getSteps() - mobile_phone selected, adding Mobile + OTP steps")

            // Step 1: Mobile Phone Verification step
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

            println("🔍 LoginStrategy.getSteps() - Total steps: ${steps.size}")
            println("🔍 LoginStrategy.getSteps() - mobilePhoneNumber: ${accumulatedFormData["mobilePhoneNumber"]}")
            println("🔍 LoginStrategy.getSteps() - fullPhoneNumber for OTP: $fullPhoneNumber")
        }

        return steps
    }

    override fun validateStep(step: Int, data: Map<String, Any>): Pair<Boolean, Map<String, String>> {
        val stepData = getSteps().getOrNull(step) ?: return Pair(false, emptyMap())
        val formData = data.mapValues { it.value.toString() }

        // ✅ CRITICAL: Validate Step 0 (Login Method Selection)
        // Ensure user has selected a registration method before proceeding
        if (step == 0) {
            // ✅ Check both current formData and accumulatedFormData
            val selectedMethod = formData["registrationMethod"] ?: accumulatedFormData["registrationMethod"]

            println("🔍 LoginStrategy.validateStep() - selectedMethod: $selectedMethod (from formData: ${formData["registrationMethod"]}, from accumulated: ${accumulatedFormData["registrationMethod"]})")

            if (selectedMethod.isNullOrBlank()) {
                return Pair(
                    false,
                    mapOf("registrationMethod" to "يجب اختيار طريقة تسجيل الدخول")
                )
            }
        }

        // ✅ Special validation for Step 1 (Mobile Phone Verification)
        // Only allow to proceed if user selected "mobile_phone" in Step 0
        if (step == 1) {
            val selectedMethod = accumulatedFormData["registrationMethod"]
            if (selectedMethod != "mobile_phone") {
                // Skip validation - this step won't be shown for civil_id users
                return Pair(true, emptyMap())
            }
        }

        // ✅ Special validation for Step 2 (OTP Verification)
        // Only allow if phone number was entered in Step 1
        if (step == 2) {
            val selectedMethod = accumulatedFormData["registrationMethod"]
            if (selectedMethod == "mobile_phone") {
                val phoneNumber = accumulatedFormData["mobilePhoneNumber"]
                if (phoneNumber.isNullOrEmpty()) {
                    return Pair(
                        false,
                        mapOf("mobilePhoneNumber" to "يجب إدخال رقم الهاتف المحمول أولاً")
                    )
                }
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
        println("📝 LoginStrategy.processStepData() - oauthTriggered: $oauthTriggered")

        // ✅ CRITICAL: Only process when "Next" button is clicked, NOT on field change!
        // This prevents OAuth from auto-triggering when user changes radio selection
        // The flag will only be set when user explicitly clicks "Next"

        // Accumulate form data
        accumulatedFormData.putAll(data)

        println("📝 LoginStrategy.processStepData() - After: $accumulatedFormData")

        // ✅ Check if user selected "civil_id" in Step 0 (Login Method Selection)
        if (step == 0) {
            // ✅ Check both data and accumulatedFormData
            val selectedMethod = data["registrationMethod"] ?: accumulatedFormData["registrationMethod"]

            println("🔍 LoginStrategy: Selected method: $selectedMethod, oauthTriggered: $oauthTriggered")
            println("🔍 LoginStrategy: data['registrationMethod']: ${data["registrationMethod"]}")
            println("🔍 LoginStrategy: accumulatedFormData['registrationMethod']: ${accumulatedFormData["registrationMethod"]}")
            println("🔍 LoginStrategy: _triggerOAuthFlow flag: ${accumulatedFormData["_triggerOAuthFlow"]}")

            // ✅ CRITICAL FIX: Reset oauthTriggered if it's true but there's no active OAuth flow
            // This happens when:
            // 1. User returns from OAuth without completing (resetOAuthFlags cleared _triggerOAuthFlow)
            // 2. User is trying to trigger OAuth again
            // If oauthTriggered=true but _triggerOAuthFlow != "true", it means the previous attempt failed
            if (oauthTriggered && accumulatedFormData["_triggerOAuthFlow"] != "true") {
                println("🔄 LoginStrategy: Resetting oauthTriggered (was true but no active OAuth flow - flag is ${accumulatedFormData["_triggerOAuthFlow"]})")
                oauthTriggered = false
                accumulatedFormData.remove("_triggerOAuthFlow") // Clean up any stale value
            }

            // ✅ ONLY trigger OAuth when Next button is clicked (processStepData is called by nextStep)
            if (selectedMethod == "civil_id" && !oauthTriggered) {
                println("🔑 LoginStrategy: User selected civil_id - triggering OAuth flow immediately")

                // ✅ Set flag to prevent re-triggering
                oauthTriggered = true

                // ✅ Trigger OAuth WebView directly for civil_id
                accumulatedFormData["_triggerOAuthFlow"] = "true"

                // Return current step - the OAuth trigger will be handled by LoginViewModel
                return step
            } else if (selectedMethod == "mobile_phone") {
                // ✅ Reset flag if user changes to mobile_phone
                oauthTriggered = false
                // ✅ Clear OAuth trigger flag
                accumulatedFormData.remove("_triggerOAuthFlow")
                println("🔄 LoginStrategy: Reset oauthTriggered for mobile_phone")
            }
        }

        // ✅ Check step type to determine what to do
        val currentStep = getSteps().getOrNull(step)

        // ✅ Handle OTP verification using StepType (for mobile_phone flow)
        if (currentStep?.stepType == StepType.OTP_VERIFICATION) {
            val otpCode = data["otpCode"]
            if (!otpCode.isNullOrEmpty() && !oauthTriggered) {
                println("📞 LoginStrategy: OTP verified on step with type OTP_VERIFICATION")
                println("🔑 LoginStrategy: OTP code: $otpCode")

                // ✅ Set flag to prevent re-triggering
                oauthTriggered = true

                // ✅ Add a flag to trigger OAuth WebView in LoginViewModel
                // The LoginViewModel.nextStep() will detect this flag and open OAuth WebView
                accumulatedFormData["_triggerOAuthFlow"] = "true"

                println("✅ LoginStrategy: OTP step processed, OAuth flow will be triggered")
            }
        }

        return step
    }

    /**
     * ✅ NEW: Reset OAuth trigger flags
     * Called when user returns from OAuth without completing login
     */
    fun resetOAuthTrigger() {
        println("🔄 LoginStrategy: Resetting OAuth trigger flags")
        println("🔄 LoginStrategy: Before reset - accumulatedFormData: $accumulatedFormData")

        oauthTriggered = false

        // ✅ CRITICAL FIX: Clear ALL accumulated data, not just specific fields
        // This ensures no stale data remains from previous attempts (mobilePhoneNumber, otpCode, etc.)
        accumulatedFormData.clear()

        println("🔄 LoginStrategy: After reset - accumulatedFormData: $accumulatedFormData")
        println("🔄 LoginStrategy: User must select registration method and start fresh")
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

    // ✅ Override getFormData to return accumulated form data (including OAuth trigger flag)
    override fun getFormData(): Map<String, String> {
        return accumulatedFormData
    }

    override fun getContext(): TransactionContext {
        TODO("Not yet implemented")
    }
}
