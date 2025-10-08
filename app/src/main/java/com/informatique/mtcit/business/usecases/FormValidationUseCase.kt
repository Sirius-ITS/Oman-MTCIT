package com.informatique.mtcit.business.usecases

import com.informatique.mtcit.R
import com.informatique.mtcit.common.FormField
import com.informatique.mtcit.common.ResourceProvider
import com.informatique.mtcit.ui.viewmodels.StepData
import javax.inject.Inject

/**
 * Use case for validating form fields
 */
class FormValidationUseCase @Inject constructor(
    private val resourceProvider: ResourceProvider
) {

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
