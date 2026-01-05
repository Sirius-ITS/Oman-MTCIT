package com.informatique.mtcit.business.usecases

import com.informatique.mtcit.R
import com.informatique.mtcit.business.validation.FormValidator
import com.informatique.mtcit.business.validation.rules.ValidationRule
import com.informatique.mtcit.common.FormField
import com.informatique.mtcit.common.ResourceProvider
import com.informatique.mtcit.ui.viewmodels.StepData
import javax.inject.Inject

/**
 * Use case for validating form fields
 */
class FormValidationUseCase @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val formValidator: FormValidator

) {
    /**
     * ✅ NEW: Validate step with accumulated form data
     * This allows cross-step validation (e.g., check IMO when validating weights)
     */
    fun validateStepWithAccumulatedData(
        stepData: StepData,
        currentStepData: Map<String, String>,
        allAccumulatedData: Map<String, String>,
        crossFieldRules: List<ValidationRule> = emptyList()
    ): Pair<Boolean, Map<String, String>> {

        // Merge accumulated data with current step data
        val combinedData = allAccumulatedData.toMutableMap()
        combinedData.putAll(currentStepData)

        // Update current step fields with their values
        val fieldsWithData = stepData.fields.map { field ->
            val value = currentStepData[field.id] ?: ""
            updateFieldValue(field, value)
        }

        // Validate with accumulated data (enables cross-step validation)
        val validatedFields = if (crossFieldRules.isNotEmpty()) {
            formValidator.validateWithAccumulatedData(
                fieldsWithData,
                combinedData,
                crossFieldRules
            )
        } else {
            formValidator.validateAll(fieldsWithData)
        }

        // Extract errors
        val errors = validatedFields
            .filter { it.error != null }
            .associate { it.id to it.error!! }

        return Pair(errors.isEmpty(), errors)
    }

    /**
     * Original method for backwards compatibility
     */
    fun validateStep(
        stepData: StepData,
        formData: Map<String, String>,
        crossFieldRules: List<ValidationRule> = emptyList()
    ): Pair<Boolean, Map<String, String>> {
        return validateStepWithAccumulatedData(
            stepData,
            formData,
            formData, // Use same data for both if not provided separately
            crossFieldRules
        )
    }

    /**
     * Helper method to update field mortgageValue based on field type
     */
    private fun updateFieldValue(field: FormField, value: String): FormField {
        return when (field) {
            is FormField.TextField -> field.copy(value = value)
            is FormField.DropDown -> field.copy(value = value)
            is FormField.CheckBox -> field.copy(checked = value.toBoolean())
            is FormField.DatePicker -> field.copy(value = value)
            is FormField.FileUpload -> field.copy(value = value)
            is FormField.OwnerList -> field.copy(value = value)
            is FormField.EngineList -> field.copy(value = value)
            is FormField.SelectableList<*> -> field.copy(value = value)
            is FormField.MarineUnitSelector -> field.copy(value = value)
            is FormField.RadioGroup -> field.copy(value = value)
            is FormField.InfoCard -> field.copy(value = value)
            is FormField.PhoneNumberField -> field.copy(value = value)
            is FormField.OTPField -> field.copy(value = value)
            is FormField.SailorList -> field.copy(value = value)
            is FormField.MultiSelectDropDown -> field.copy(value = value)
            is FormField.PaymentDetails -> field.copy(value = value)
        }
    }

    /**
     * Validates all fields in a step and returns validation result
     * @return Pair<Boolean, Map<String, String>> - isValid and field errors
     */
    fun validateStep(
        stepData: StepData,
        formData: Map<String, String>
    ): Pair<Boolean, Map<String, String>> {
        val errors = mutableMapOf<String, String>()
        var isValid = true

        stepData.fields.forEach { field ->
            if (field.mandatory && shouldValidateField(field, formData)) {
                val value = formData[field.id] ?: ""

                val errorMessage = when {
                    // Check if field is empty or checkbox is not checked
                    value.isEmpty() || (field is FormField.CheckBox && value != "true") -> {
                        resourceProvider.getString(R.string.field_required_error)
                    }
                    // Email validation
                    field.id.contains("email", ignoreCase = true) && value.isNotEmpty() && !isValidEmail(value) -> {
                        "البريد الإلكتروني غير صالح"
                    }
                    // Phone validation
                    field.id.contains("mobile", ignoreCase = true) || field.id.contains("phone", ignoreCase = true) -> {
                        if (!isValidPhone(value)) "رقم الهاتف غير صالح" else null
                    }
                    // Numeric field validation
                    field is FormField.TextField && field.isNumeric && value.isNotEmpty() && !value.all { it.isDigit() } -> {
                        "يجب أن يحتوي على أرقام فقط"
                    }
                    else -> null
                }

                if (errorMessage != null) {
                    errors[field.id] = errorMessage
                    isValid = false
                }
            }
        }

        return isValid to errors
    }



    /**
     * Checks if all mandatory fields are filled (without validating their correctness)
     * This is used to enable/disable the Next button
     * @return Boolean - true if all mandatory fields have values
     */
    fun areMandatoryFieldsFilled(
        stepData: StepData,
        formData: Map<String, String>
    ): Boolean {
        stepData.fields.forEach { field ->
            if (field.mandatory && shouldValidateField(field, formData)) {
                val value = formData[field.id] ?: ""

                // Check if field is empty
                if (value.isEmpty()) {
                    return false
                }

                // ✅ Special validation for marine unit selection
                // User must select at least one ship (array should not be empty)
                if (field.id == "selectedMarineUnits") {
                    // Check if it's an empty JSON array or contains actual selections
                    if (value == "[]" || value.isBlank()) {
                        println("❌ selectedMarineUnits is empty - Next button should be disabled")
                        return false
                    }
                    println("✅ selectedMarineUnits has selection: $value")
                }

                // ✅ Special validation for crew/sailor step
                // User must either upload Excel file OR add sailors manually
                if (field.id == "sailors") {
                    val hasExcelFile = formData["crewExcelFile"]?.isNotBlank() == true
                    val hasSailors = value != "[]" && value.isNotBlank()

                    if (!hasExcelFile && !hasSailors) {
                        println("❌ No crew data - neither Excel file nor manual sailors")
                        return false
                    }
                    println("✅ Crew data provided: hasExcelFile=$hasExcelFile, hasSailors=$hasSailors")
                }

                // For checkboxes, check if they're checked
                if (field is FormField.CheckBox && value != "true") {
                    return false
                }
            }
        }

        return true
    }

    /**
     * Determines if a field should be validated based on conditional logic
     */
    private fun shouldValidateField(field: FormField, formData: Map<String, String>): Boolean {
        return when (field.id) {
            "companyName", "companyRegistrationNumber", "companyType" -> {
                formData["isCompany"] == "true"
            }
            else -> true
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isValidPhone(phone: String): Boolean {
        return phone.length >= 8 && phone.all { it.isDigit() }
    }
}
